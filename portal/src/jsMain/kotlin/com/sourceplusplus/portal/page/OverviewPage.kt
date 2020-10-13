package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.extensions.eb
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.OverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.UpdateEndpoints
import com.sourceplusplus.protocol.artifact.endpoint.EndpointResult
import com.sourceplusplus.protocol.artifact.endpoint.EndpointTableType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.PageType.*
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import kotlinx.browser.document
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import org.w3c.dom.Element
import kotlin.js.json

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class OverviewPage(
    override val portalUuid: String,
    override val externalPortal: Boolean = false
) : IOverviewPage {

    init {
        console.log("Overview tab started")

        @Suppress("EXPERIMENTAL_API_USAGE")
        eb.onopen = {
            js("portalConnected()")

            eb.registerHandler(UpdateEndpoints(portalUuid)) { _: String, message: dynamic ->
                displayEndpoints(Json.decodeFromDynamic(message.body))
            }

            eb.publish(OverviewTabOpened, json("portalUuid" to portalUuid))
        }
    }

    fun renderPage() {
        println("Rending Overview page")
        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""

        root.append {
            portalNav {
                navItem(OVERVIEW, isActive = true)
                navItem(ACTIVITY)
                navItem(TRACES) {
                    navSubItem(LATEST_TRACES, SLOWEST_TRACES, FAILED_TRACES)
                }
                navItem(CONFIGURATION)
            }
            pusherContent {
                navBar {
                    timeDropdown(*QueryTimeFrame.values()) { updateTime(it) }
                    //calendar()

                    rightAlign {
                        externalPortalButton()
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
        console.log("first endpoint: " + endpointResult.endpointMetrics[0].artifactQualifiedName)
        val root: Element = document.getElementById("endpoint_body_table")!!
        root.append {
            endpointResult.endpointMetrics.forEach {
                tr {
                    td {
                        span {
                            i("far fa-globe") {
                                style = "font-size:1.5em;margin-right:5px"
                            }
                            +it.artifactQualifiedName.operationName!!
                        }
                    }
                    td {
                        +"HTTP" //todo: dynamic
                    }
                    it.artifactSummarizedMetrics.forEach {
                        td {
                            +it.value
                        }
                    }
                }
            }
        }
    }

    override fun updateTime(interval: QueryTimeFrame) {
    }
}
