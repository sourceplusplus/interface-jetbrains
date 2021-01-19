package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.PortalBundle.translate
import com.sourceplusplus.portal.clickedTracesOrderType
import com.sourceplusplus.portal.clickedViewAsExternalPortal
import com.sourceplusplus.portal.extensions.echarts
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.toMoment
import com.sourceplusplus.portal.model.ChartItemType.*
import com.sourceplusplus.portal.setActiveTime
import com.sourceplusplus.portal.setCurrentPage
import com.sourceplusplus.protocol.portal.PageType.*
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.ActivityTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetActiveChartMetric
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetMetricTimeFrame
import com.sourceplusplus.protocol.ProtocolAddress.Portal.ClearActivity
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayCard
import com.sourceplusplus.protocol.ProtocolAddress.Portal.UpdateChart
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.metrics.BarTrendCard
import com.sourceplusplus.protocol.artifact.metrics.MetricType
import com.sourceplusplus.protocol.artifact.metrics.SplineChart
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.PortalConfiguration
import kotlinx.browser.document
import kotlinx.browser.window
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
                "top" to 20,
                "bottom" to 30,
                "left" to 55,
                "right" to 0
            ),
            "tooltip" to json(
                "trigger" to "axis",
                "formatter" to tooltipFormatter,
                "axisPointer" to json(
                    "animation" to false
                )
            ),
            "xAxis" to json(
                "type" to "time",
                "splitLine" to json(
                    "show" to true
                ),
                "axisLabel" to json(
                    "formatter" to axisFormatter,
                    "color" to labelColor
                )
            ),
            "yAxis" to json(
                "type" to "value",
                "boundaryGap" to arrayOf(0, "100%"),
                "splitLine" to json(
                    "show" to false
                ),
                "axisLabel" to json(
                    "color" to labelColor
                )
            ),
            "series" to arrayOf(series0, series1, series2, series3, series4, regressionSeries)
        )
    }

    @ExperimentalSerializationApi
    override fun setupEventbus() {
        if (!setup) {
            setup = true
            eb.registerHandler(ClearActivity(portalUuid)) { _: dynamic, _: dynamic ->
                clearActivity()
            }
            eb.registerHandler(DisplayCard(portalUuid)) { _: dynamic, message: dynamic ->
                displayCard(Json.decodeFromDynamic(message.body))
            }
            eb.registerHandler(UpdateChart(portalUuid)) { _: dynamic, message: dynamic ->
                updateChart(Json.decodeFromDynamic(message.body))
            }

            eb.publish(ActivityTabOpened, json("portalUuid" to portalUuid))
        }
        eb.send(RefreshPortal, portalUuid)
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
                if (configuration.visibleTraces) navItem(TRACES, false, null) {
                    navSubItems(
                        PortalNavSubItem(LATEST_TRACES) { clickedTracesOrderType(eb, portalUuid, LATEST_TRACES) },
                        PortalNavSubItem(SLOWEST_TRACES) { clickedTracesOrderType(eb, portalUuid, SLOWEST_TRACES) },
                        PortalNavSubItem(FAILED_TRACES) { clickedTracesOrderType(eb, portalUuid, FAILED_TRACES) }
                    )
                }
                if (configuration.visibleConfiguration) navItem(CONFIGURATION, onClick = {
                    setCurrentPage(eb, portalUuid, CONFIGURATION)
                })
            }
            activityContent {
                navBar {
                    timeDropdown(*QueryTimeFrame.values()) { updateTime(it) }
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

    fun loadChart() {
        console.log("Loading chart")
        overviewChart = echarts.init(document.getElementById("overview_chart"))
        window.onresize = {
            console.log("Resizing overview chart")
            overviewChart.resize()
        }
        overviewChart.setOption(overviewChartOptions)
    }

    fun clearActivity() {
        console.log("Clearing activity")

        series0["data"] = arrayOf<Int>()
        series1["data"] = arrayOf<Int>()
        series2["data"] = arrayOf<Int>()
        series3["data"] = arrayOf<Int>()
        series4["data"] = arrayOf<Int>()
        regressionSeries["data"] = arrayOf<Int>()

        overviewChartOptions["series"] = emptyArray<kotlin.js.Json>()
        overviewChart.setOption(overviewChartOptions)
        overviewChart.resize()
    }

    override fun displayCard(card: BarTrendCard) {
        console.log("Displaying card. Type: ${card.meta}")
        setActiveTime(card.timeFrame)

        document.getElementById("card_${card.meta.toLowerCase()}_header")!!.textContent = translate(card.header)
    }

    override fun updateChart(chartData: SplineChart) {
        console.log("Updating chart")
        setActiveTime(chartData.timeFrame)

        val cards = listOf("throughput_average", "responsetime_average", "servicelevelagreement_average")
        for (i in cards.indices) {
            jq("#card_" + cards[i] + "_header").removeClass("spp_red_color")
            jq("#card_" + cards[i] + "_header_label").removeClass("spp_red_color")
        }
        jq("#card_" + chartData.metricType.name.toLowerCase() + "_header").addClass("spp_red_color")
        jq("#card_" + chartData.metricType.name.toLowerCase() + "_header_label").addClass("spp_red_color")
        if (chartData.metricType.name.toLowerCase() == cards[0]) {
            tooltipMeasurement = "/" + translate("min")
        } else if (chartData.metricType.name.toLowerCase() == cards[1]) {
            tooltipMeasurement = translate("ms")
        } else if (chartData.metricType.name.toLowerCase() == cards[2]) {
            tooltipMeasurement = "%"
        }

        for (i in chartData.seriesData.indices) {
            val seriesData = chartData.seriesData[i]
            val list = mutableListOf<kotlin.js.Json>()
            for (z in seriesData.values.indices) {
                val value = seriesData.values[z]
                val time = seriesData.times[z].toMoment().valueOf()

                list.add(
                    json(
                        "value" to arrayOf(time, value),
                        "itemStyle" to json(
                            "normal" to json(
                                "color" to symbolColor
                            )
                        )
                    )
                )

                if (seriesData.seriesIndex == 0) {
                    series0["data"] = list.toTypedArray()
                } else if (seriesData.seriesIndex == 1) {
                    series1["data"] = list.toTypedArray()
                } else if (seriesData.seriesIndex == 2) {
                    series2["data"] = list.toTypedArray()
                } else if (seriesData.seriesIndex == 3) {
                    series3["data"] = list.toTypedArray()
                } else if (seriesData.seriesIndex == 4) {
                    series4["data"] = list.toTypedArray()
                } else if (seriesData.seriesIndex == 5) {
                    regressionSeries["data"] = list.toTypedArray()
                }
            }
        }

        overviewChart.setOption(
            json(
                "series" to arrayOf(
                    series0,
                    series1,
                    series2,
                    series3,
                    series4,
                    regressionSeries
                )
            )
        )
    }

    override fun updateTime(interval: QueryTimeFrame) {
        console.log("Update time: $interval")
        eb.send(
            SetMetricTimeFrame,
            json(
                "portalUuid" to portalUuid,
                "metricTimeFrame" to interval.name
            )
        )

        setActiveTime(interval)
    }

    private fun clickedViewAverageThroughputChart() {
        console.log("Clicked view average throughput")
        eb.send(
            SetActiveChartMetric,
            json(
                "portalUuid" to portalUuid,
                "metricType" to MetricType.Throughput_Average.name
            )
        )
    }

    private fun clickedViewAverageResponseTimeChart() {
        console.log("Clicked view average response time")
        eb.send(
            SetActiveChartMetric,
            json(
                "portalUuid" to portalUuid,
                "metricType" to MetricType.ResponseTime_Average.name
            )
        )
    }

    private fun clickedViewAverageSLAChart() {
        console.log("Clicked view average SLA")
        eb.send(
            SetActiveChartMetric,
            json(
                "portalUuid" to portalUuid,
                "metricType" to MetricType.ServiceLevelAgreement_Average.name
            )
        )
    }

    companion object {
        val series0 = json(
            "name" to translate("99th percentile"),
            "type" to "line",
            "color" to "#e1483b",
            "hoverAnimation" to false,
            "symbol" to "circle",
            "symbolSize" to 8,
            "showSymbol" to true,
            "areaStyle" to {},
            "data" to arrayOf<Int>()
        )
        val series1 = json(
            "name" to translate("95th percentile"),
            "type" to "line",
            "color" to "#e1483b",
            "showSymbol" to false,
            "hoverAnimation" to false,
            "areaStyle" to {},
            "data" to arrayOf<Int>()
        )
        val series2 = json(
            "name" to translate("90th percentile"),
            "type" to "line",
            "color" to "#e1483b",
            "showSymbol" to false,
            "hoverAnimation" to false,
            "areaStyle" to {},
            "data" to arrayOf<Int>()
        )
        val series3 = json(
            "name" to translate("75th percentile"),
            "type" to "line",
            "color" to "#e1483b",
            "showSymbol" to false,
            "hoverAnimation" to false,
            "areaStyle" to {},
            "data" to arrayOf<Int>()
        )
        val series4 = json(
            "name" to translate("50th percentile"),
            "type" to "line",
            "color" to "#e1483b",
            "showSymbol" to false,
            "hoverAnimation" to false,
            "areaStyle" to {},
            "data" to arrayOf<Int>()
        )
        val regressionSeries = json(
            "name" to translate("Predicted Regression"),
            "type" to "line",
            "color" to "#000",
            "showSymbol" to true,
            "hoverAnimation" to false,
            "symbol" to "square",
            "symbolSize" to 8,
            "areaStyle" to {},
            "data" to arrayOf<Int>()
        )
    }
}
