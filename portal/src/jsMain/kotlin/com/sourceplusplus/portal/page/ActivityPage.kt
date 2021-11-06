package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.*
import com.sourceplusplus.portal.PortalBundle.translate
import com.sourceplusplus.portal.extensions.echarts
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.toFixed
import com.sourceplusplus.portal.extensions.toMoment
import com.sourceplusplus.portal.model.ChartItemType.*
import com.sourceplusplus.portal.template.*
import spp.protocol.ProtocolAddress.Global.ActivityTabOpened
import spp.protocol.ProtocolAddress.Global.RefreshPortal
import spp.protocol.ProtocolAddress.Global.SetActiveChartMetric
import spp.protocol.ProtocolAddress.Global.SetMetricTimeFrame
import spp.protocol.ProtocolAddress.Portal.ClearActivity
import spp.protocol.ProtocolAddress.Portal.DisplayActivity
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.log.LogOrderType.NEWEST_LOGS
import spp.protocol.artifact.metrics.ArtifactMetricResult
import spp.protocol.artifact.metrics.MetricType.*
import spp.protocol.artifact.trace.TraceOrderType.*
import spp.protocol.portal.PageType.*
import spp.protocol.portal.PortalConfiguration
import spp.protocol.utils.fromPerSecondToPrettyFrequency
import spp.protocol.utils.toPrettyDuration
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.dom.addClass
import kotlinx.html.dom.append
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import moment
import org.w3c.dom.Element
import kotlin.js.json

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActivityPage(
    override val portalUuid: String,
    private val eb: EventBus
) : IActivityPage() {

    private var overviewChart: dynamic = null
    private val tooltipFormatter: ((params: dynamic) -> String) = { params ->
        val time = params[0].value[0].toString()
        val measurement = params[0].value[1].toString().toDouble()
        moment(time, "x").format("LTS") + " : " + measurement + translate(tooltipMeasurement)
    }
    private val axisFormatter: ((value: dynamic) -> String) = { value ->
        moment(value.toString(), "x").format("LT")
    }
    private val overviewChartOptions by lazy {
        json(
            "grid" to json(
                "top" to 10,
                "bottom" to 5,
                "left" to 10,
                "right" to 0,
                "containLabel" to true
            ),
            "tooltip" to json(
                "trigger" to "axis",
                "formatter" to tooltipFormatter
            ),
            "xAxis" to json(
                "type" to "time",
                "splitLine" to json(
                    "show" to true
                ),
                "axisLabel" to json(
                    "formatter" to axisFormatter,
                    "color" to labelColor
                ),
                "boundaryGap" to false
            ),
            "yAxis" to json(
                "type" to "value",
                "boundaryGap" to false,
                "splitLine" to json(
                    "show" to false
                ),
                "axisLabel" to json(
                    "color" to labelColor
                )
            ),
            "series" to arrayOf(
                series_avg,
                series_99p,
                series_95p,
                series_90p,
                series_75p,
                series_50p,
                regressionSeries
            )
        )
    }

    @ExperimentalSerializationApi
    override fun setupEventbus() {
        if (!setup) {
            setup = true
            eb.registerHandler(ClearActivity(portalUuid)) { _: dynamic, _: dynamic ->
                clearActivity()
            }
            eb.registerHandler(DisplayActivity(portalUuid)) { _: dynamic, message: dynamic ->
                displayActivity(Json.decodeFromDynamic(message.body))
            }

            eb.publish(ActivityTabOpened, json("portalUuid" to portalUuid))
        }
        eb.publish(RefreshPortal, portalUuid)
    }

    override fun renderPage(portalConfiguration: PortalConfiguration) {
        console.log("Rending Activity page")
        this.configuration = portalConfiguration

        document.title = translate("Activity - SourceMarker")
        val root: Element = document.getElementById("root")!!
        root.addClass("overflow_y_hidden")
        root.innerHTML = ""
        root.append {
            portalNav {
                if (configuration.visibleOverview) navItem(OVERVIEW, onClick = {
                    setCurrentPage(eb, portalUuid, OVERVIEW)
                })
                if (configuration.visibleActivity) navItem(ACTIVITY, isActive = true, onClick = {
                    setCurrentPage(eb, portalUuid, ACTIVITY)
                })
                if (configuration.visibleTraces) navItem(TRACES, block = {
                    navSubItems(
                        PortalNavSubItem(LATEST_TRACES) { clickedTracesOrderType(eb, portalUuid, LATEST_TRACES) },
                        PortalNavSubItem(SLOWEST_TRACES) { clickedTracesOrderType(eb, portalUuid, SLOWEST_TRACES) },
                        PortalNavSubItem(FAILED_TRACES) { clickedTracesOrderType(eb, portalUuid, FAILED_TRACES) }
                    )
                })
                if (configuration.visibleLogs) navItem(LOGS, onClick = {
                    clickedLogsOrderType(eb, portalUuid, NEWEST_LOGS)
                })
                if (configuration.visibleConfiguration) navItem(CONFIGURATION, onClick = {
                    setCurrentPage(eb, portalUuid, CONFIGURATION)
                })
            }
            activityContent {
                navBar {
                    timeDropdown(
                        *QueryTimeFrame.values().filterNot { it == QueryTimeFrame.LAST_MINUTE }.toTypedArray()
                    ) {
                        eb.send(
                            SetMetricTimeFrame,
                            json("portalUuid" to portalUuid, "metricTimeFrame" to it.name)
                        )
                    }
                    //calendar()

                    rightAlign {
                        externalPortalButton { clickedViewAsExternalPortal(eb, portalUuid) }
                    }
                }
                areaChart {
                    chartItem(AVG_THROUGHPUT) { clickedViewAverageThroughputChart() }
                    chartItem(AVG_RESPONSE_TIME, isActive = true) { clickedViewAverageResponseTimeChart() }
                    chartItem(AVG_SLA) { clickedViewAverageSLAChart() }
                }
            }
        }

        loadChart()
    }

    private fun loadChart() {
        console.log("Loading chart")
        overviewChart = echarts.init(document.getElementById("overview_chart"))
        window.onresize = {
            console.log("Resizing overview chart")
            overviewChart.resize()
        }
        overviewChart.setOption(overviewChartOptions)
    }

    private fun clearActivity() {
        console.log("Clearing activity")

        overviewChartOptions["series"] = emptyArray<kotlin.js.Json>()
        overviewChart.setOption(overviewChartOptions)
        overviewChart.resize()
    }

    override fun displayActivity(metricResult: ArtifactMetricResult) {
        console.log("Updating chart")
        setActiveTime(metricResult.timeFrame)

        val cards = listOf("throughput_average", "responsetime_average", "servicelevelagreement_average")
        for (i in cards.indices) {
            jq("#card_" + cards[i] + "_header").removeClass("spp_red_color")
            jq("#card_" + cards[i] + "_header_label").removeClass("spp_red_color")
        }
        jq("#card_" + metricResult.focus.name.toLowerCase() + "_header").addClass("spp_red_color")
        jq("#card_" + metricResult.focus.name.toLowerCase() + "_header_label").addClass("spp_red_color")
        when {
            metricResult.focus.name.toLowerCase() == cards[0] -> tooltipMeasurement = "/" + translate("min")
            metricResult.focus.name.toLowerCase() == cards[1] -> tooltipMeasurement = translate("ms")
            metricResult.focus.name.toLowerCase() == cards[2] -> tooltipMeasurement = "%"
        }

        for (chartData in metricResult.artifactMetrics) {
            when (chartData.metricType) {
                Throughput_Average -> {
                    val meta = chartData.metricType.toString().toLowerCase()
                    document.getElementById("card_${meta}_header")!!.textContent =
                        (chartData.values.average() / 60.0).fromPerSecondToPrettyFrequency()
                }
                ResponseTime_Average -> {
                    val meta = chartData.metricType.toString().toLowerCase()
                    document.getElementById("card_${meta}_header")!!.textContent =
                        chartData.values.average().toInt().toPrettyDuration()
                }
                ServiceLevelAgreement_Average -> {
                    val meta = chartData.metricType.toString().toLowerCase()
                    val avg = chartData.values.average()
                    document.getElementById("card_${meta}_header")!!.textContent =
                        if (avg == 0.0) "0%" else (avg / 100.0).toFixed(1) + "%"
                }
                else -> {
                    console.log("Ignoring unknown metric type: ${chartData.metricType}")
                }
            }

            if (chartData.metricType == metricResult.focus) {
                var current = metricResult.start
                val focusedSeries = mutableListOf<kotlin.js.Json>()
                for (i in chartData.values.indices) {
                    val value = if (chartData.metricType == ServiceLevelAgreement_Average) {
                        chartData.values[i] / 100 //transforms to 0%-100%
                    } else {
                        chartData.values[i]
                    }
                    focusedSeries.add(
                        json(
                            "value" to arrayOf(current.toMoment().valueOf(), value),
                            "itemStyle" to json(
                                "normal" to json(
                                    "color" to symbolColor
                                )
                            )
                        )
                    )

                    if (metricResult.step == "MINUTE") {
                        current = current.plus(DateTimeUnit.Companion.MINUTE, TimeZone.UTC)
                    } else {
                        throw UnsupportedOperationException("Invalid step: " + metricResult.step)
                    }
                }

                when (chartData.metricType) {
                    Throughput_Average -> series_avg["data"] = focusedSeries.toTypedArray()
                    ResponseTime_Average -> series_avg["data"] = focusedSeries.toTypedArray()
                    ServiceLevelAgreement_Average -> series_avg["data"] = focusedSeries.toTypedArray()
                    ResponseTime_99Percentile -> series_99p["data"] = focusedSeries.toTypedArray()
                    ResponseTime_95Percentile -> series_95p["data"] = focusedSeries.toTypedArray()
                    ResponseTime_90Percentile -> series_90p["data"] = focusedSeries.toTypedArray()
                    ResponseTime_75Percentile -> series_75p["data"] = focusedSeries.toTypedArray()
                    ResponseTime_50Percentile -> series_50p["data"] = focusedSeries.toTypedArray()
                }

                overviewChart.setOption(
                    json(
                        "series" to arrayOf(
                            series_avg,
                            series_99p,
                            series_95p,
                            series_90p,
                            series_75p,
                            series_50p,
                            regressionSeries
                        )
                    )
                )
            }
        }
    }

    private fun clickedViewAverageThroughputChart() {
        console.log("Clicked view average throughput")
        eb.send(
            SetActiveChartMetric,
            json(
                "portalUuid" to portalUuid,
                "metricType" to Throughput_Average.name
            )
        )
    }

    private fun clickedViewAverageResponseTimeChart() {
        console.log("Clicked view average response time")
        eb.send(
            SetActiveChartMetric,
            json(
                "portalUuid" to portalUuid,
                "metricType" to ResponseTime_Average.name
            )
        )
    }

    private fun clickedViewAverageSLAChart() {
        console.log("Clicked view average SLA")
        eb.send(
            SetActiveChartMetric,
            json(
                "portalUuid" to portalUuid,
                "metricType" to ServiceLevelAgreement_Average.name
            )
        )
    }

    companion object {
        val series_avg = json(
            "name" to translate("Average"),
            "type" to "line",
            "color" to "#e1483b",
            "symbol" to "circle",
            "symbolSize" to 8,
            "showSymbol" to true,
            "areaStyle" to {},
            "data" to arrayOf<Int>()
        )
        val series_99p = json(
            "name" to translate("99th percentile"),
            "type" to "line",
            "showSymbol" to false,
            "data" to arrayOf<Int>()
        )
        val series_95p = json(
            "name" to translate("95th percentile"),
            "type" to "line",
            "showSymbol" to false,
            "data" to arrayOf<Int>()
        )
        val series_90p = json(
            "name" to translate("90th percentile"),
            "type" to "line",
            "showSymbol" to false,
            "data" to arrayOf<Int>()
        )
        val series_75p = json(
            "name" to translate("75th percentile"),
            "type" to "line",
            "showSymbol" to false,
            "data" to arrayOf<Int>()
        )
        val series_50p = json(
            "name" to translate("50th percentile"),
            "type" to "line",
            "showSymbol" to false,
            "data" to arrayOf<Int>()
        )
        val regressionSeries = json(
            "name" to translate("Predicted Regression"),
            "type" to "line",
            "color" to "#000",
            "showSymbol" to true,
            "symbol" to "square",
            "symbolSize" to 8,
            "data" to arrayOf<Int>()
        )
    }
}
