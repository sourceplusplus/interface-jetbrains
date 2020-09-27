package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.extensions.eb
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.OverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.SetActiveChartMetric
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.ClearOverview
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayCard
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.UpdateChart
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.ChartItemType.*
import com.sourceplusplus.protocol.portal.MetricType
import com.sourceplusplus.protocol.portal.PageType.*
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.html.dom.append
import org.w3c.dom.Element
import kotlin.js.json

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class OverviewPage {

    val portalUuid = "null"
    var currentMetricType: MetricType = MetricType.Throughput_Average
    var currentTimeFrame = QueryTimeFrame.LAST_5_MINUTES

    init {
        console.log("Overview tab started")
        console.log("Connecting portal")
        eb.onopen = {
            js("portalConnected()")
            clickedViewAverageResponseTimeChart() //default = avg resp time

            eb.registerHandler(ClearOverview(portalUuid)) { error: String, message: Any ->
                js("clearOverview();")
            }
            eb.registerHandler(DisplayCard(portalUuid)) { error: String, message: Any ->
                js("displayCard(message.body);")
            }
            eb.registerHandler(UpdateChart(portalUuid)) { error: String, message: Any ->
                js("updateChart(message.body);")
            }

            var timeFrame = localStorage.getItem("spp.metric_time_frame")
            if (timeFrame == null) {
                timeFrame = currentTimeFrame.name
                localStorage.setItem("spp.metric_time_frame", timeFrame)
            }
            updateTime(QueryTimeFrame.valueOf(timeFrame.toUpperCase()))
            js("portalLog('Set initial time frame to: ' + timeFrame);")

            eb.publish(OverviewTabOpened, "{'portal_uuid': '$portalUuid'}")
        }
    }

    fun renderPage() {
        println("Rending Overview page")
        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""

        root.append {
            portalNav {
                navItem(OVERVIEW, isActive = true)
                navItem(TRACES) {
                    navSubItem(LATEST_TRACES, SLOWEST_TRACES, FAILED_TRACES)
                }
                navItem(CONFIGURATION)
            }
            overviewContent {
                navBar {
                    timeDropdown(*QueryTimeFrame.values()) { updateTime(it) }
                    calendar()

                    rightAlign {
                        externalPortalButton()
                    }
                }
                areaChart {
                    chartItem(AVG_THROUGHPUT) { clickedViewAverageThroughputChart() }
                    chartItem(AVG_RESPONSE_TIME, isActive = true) { clickedViewAverageResponseTimeChart() }
                    chartItem(AVG_SLA) { clickedViewAverageSLAChart() }
                }
            }
        }

        js("loadChart();")
    }

    private fun updateTime(interval: QueryTimeFrame) {
        console.log("Update time: $interval")
        currentTimeFrame = interval
        localStorage.setItem("spp.metric_time_frame", interval.name)
        eb.send(
            "SetMetricTimeFrame",
            json(
                "portal_uuid" to portalUuid,
                "metric_time_frame" to interval.name
            )
        )

        jq("#last_5_minutes_time").removeClass("active")
        jq("#last_15_minutes_time").removeClass("active")
        jq("#last_30_minutes_time").removeClass("active")
        jq("#last_hour_time").removeClass("active")
        jq("#last_3_hours_time").removeClass("active")

        jq("#" + interval.name.toLowerCase() + "_time").addClass("active")
    }

    private fun clickedViewAverageThroughputChart() {
        console.log("Clicked view average throughput")
        currentMetricType = MetricType.Throughput_Average
        eb.send(
            SetActiveChartMetric,
            json(
                "portal_uuid" to portalUuid,
                "metric_type" to currentMetricType.name
            )
        )
    }

    private fun clickedViewAverageResponseTimeChart() {
        console.log("Clicked view average response time")
        currentMetricType = MetricType.ResponseTime_Average
        eb.send(
            SetActiveChartMetric,
            json(
                "portal_uuid" to portalUuid,
                "metric_type" to currentMetricType.name
            )
        )
    }

    private fun clickedViewAverageSLAChart() {
        console.log("Clicked view average SLA")
        currentMetricType = MetricType.ServiceLevelAgreement_Average
        eb.send(
            SetActiveChartMetric,
            json(
                "portal_uuid" to portalUuid,
                "metric_type" to currentMetricType.name
            )
        )
    }
}
