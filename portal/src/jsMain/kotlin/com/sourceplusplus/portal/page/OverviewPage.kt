package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.*
import com.sourceplusplus.portal.PortalBundle.translate
import com.sourceplusplus.portal.model.EndpointTableType
import com.sourceplusplus.portal.template.*
import spp.protocol.ProtocolAddress.Global.ClickedEndpointArtifact
import spp.protocol.ProtocolAddress.Global.RefreshPortal
import spp.protocol.ProtocolAddress.Global.SetOverviewTimeFrame
import spp.protocol.ProtocolAddress.Portal.UpdateEndpoints
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.endpoint.EndpointResult
import spp.protocol.artifact.endpoint.EndpointType
import spp.protocol.artifact.log.LogOrderType.NEWEST_LOGS
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.artifact.trace.TraceOrderType.*
import spp.protocol.portal.PageType.*
import spp.protocol.portal.PortalConfiguration
import spp.protocol.utils.fromPerSecondToPrettyFrequency
import spp.protocol.utils.toPrettyDuration
import kotlinx.browser.document
import kotlinx.dom.clear
import kotlinx.dom.removeClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.json.encodeToDynamic
import org.w3c.dom.Element
import kotlin.js.json

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class OverviewPage(
    override val portalUuid: String,
    private val eb: EventBus
) : IOverviewPage() {

    override fun setupEventbus() {
        if (!setup) {
            setup = true
            eb.registerHandler(UpdateEndpoints(portalUuid)) { _: dynamic, message: dynamic ->
                displayEndpoints(Json.decodeFromDynamic(message.body))
            }
        }
        eb.publish(RefreshPortal, portalUuid)
    }

    override fun renderPage(portalConfiguration: PortalConfiguration) {
        console.log("Rending Overview page")
        this.configuration = portalConfiguration

        document.title = translate("Overview - SourceMarker")
        val root: Element = document.getElementById("root")!!
        root.removeClass("overflow_y_hidden")
        root.innerHTML = ""
        root.append {
            portalNav {
                if (configuration.visibleOverview) navItem(OVERVIEW, isActive = true, onClick = {
                    setCurrentPage(eb, portalUuid, OVERVIEW)
                })
                if (configuration.visibleActivity) navItem(ACTIVITY, onClick = {
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
            portalContent {
                navBar {
                    timeDropdown(*QueryTimeFrame.values()) { updateTime(it) }
                    //calendar()

                    rightAlign {
                        externalPortalButton { clickedViewAsExternalPortal(eb, portalUuid) }
                    }
                }
                wideColumn {
                    table(
                        "secondary_background_color no_top_margin",
                        "endpoint_table", "endpoint_body_table",
                        tableTypes = EndpointTableType.values()
                    )
                }
            }
        }
    }

    override fun displayEndpoints(endpointResult: EndpointResult) {
        console.log("Displaying endpoints")
        val root: Element = document.getElementById("endpoint_body_table")!!
        root.clear()

        root.append {
            endpointResult.endpointMetrics.forEach {
                val slaPercent = it.artifactSummarizedMetrics.find {
                    it.metricType == MetricType.ServiceLevelAgreement_Average
                }!!.value / 100.0

                tr {
                    onClickFunction = { _ ->
                        eb.send(
                            ClickedEndpointArtifact, json(
                                "portalUuid" to portalUuid,
                                "artifactQualifiedName" to Json.encodeToDynamic(it.artifactQualifiedName)
                            )
                        )
                    }
                    if (slaPercent <= 60.0) {
                        classes += "negative"
                    }

                    td("overview_row_padding") {
                        if (it.endpointType == EndpointType.HTTP) {
                            val color2 = Color(24, 45, 52)
                            val color1 = Color(225, 72, 59)
                            val r: Double = color1.red + slaPercent * (color2.red - color1.red)
                            val g: Double = color1.green + slaPercent * (color2.green - color1.green)
                            val b: Double = color1.blue + slaPercent * (color2.blue - color1.blue)

                            span {
                                i("far fa-globe-americas") {
                                    style = "font-size:1.5em; margin-right:5px; color:rgb($r, $g, $b)"
                                }
                            }
                            span {
                                style = "vertical-align:top"
                                val httpOperation = it.artifactQualifiedName.operationName!!
                                when {
                                    httpOperation.startsWith("{GET}") -> {
                                        span {
                                            style = "font-weight: bold"
                                            +"[${translate("GET")}] "
                                        }
                                        +httpOperation.substring(5)
                                    }
                                    httpOperation.startsWith("{PUT}") -> {
                                        span {
                                            style = "font-weight: bold"
                                            +"[${translate("PUT")}] "
                                        }
                                        +httpOperation.substring(5)
                                    }
                                    httpOperation.startsWith("{POST}") -> {
                                        span {
                                            style = "font-weight: bold"
                                            +"[${translate("POST")}] "
                                        }
                                        +httpOperation.substring(6)
                                    }
                                    httpOperation.startsWith("{PATCH}") -> {
                                        span {
                                            style = "font-weight: bold"
                                            +"[${translate("PATCH")}] "
                                        }
                                        +translate(httpOperation.substring(7))
                                    }
                                    httpOperation.startsWith("{DELETE}") -> {
                                        span {
                                            style = "font-weight: bold"
                                            +"[${translate("DELETE")}] "
                                        }
                                        +httpOperation.substring(8)
                                    }
                                    else -> +translate(httpOperation)
                                }
                            }
                        } else {
                            +it.artifactQualifiedName.operationName!!
                        }
                    }
                    td("overview_row_padding collapsing") {
                        style = "color: #53A889; font-weight: bold"
                        +translate(it.endpointType.name)
                    }
                    it.artifactSummarizedMetrics.forEach {
                        val summaryValue = when (it.metricType) {
                            MetricType.Throughput_Average -> (it.value / 60.0).fromPerSecondToPrettyFrequency()
                            MetricType.ResponseTime_Average -> it.value.toInt().toPrettyDuration()
                            MetricType.ServiceLevelAgreement_Average -> {
                                Double
                                if (it.value == 0.0) "0%" else (it.value / 100.0).asDynamic().toFixed(2) + "%"
                            }
                            else -> throw UnsupportedOperationException("Unable to format: ${it.metricType}")
                        } as String

                        td("overview_row_padding collapsing") {
                            attributes["data-sort-value"] = it.value.toString()
                            +summaryValue
                        }
                    }
                }
            }
        }
    }

    override fun updateTime(interval: QueryTimeFrame) {
        console.log("Update time: $interval")
        eb.send(
            SetOverviewTimeFrame,
            json(
                "portalUuid" to portalUuid,
                "queryTimeFrame" to interval.name
            )
        )

        setActiveTime(interval)
    }

    private data class Color(
        val red: Int,
        val blue: Int,
        val green: Int
    )
}
