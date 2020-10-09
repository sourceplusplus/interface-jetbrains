package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.extensions.eb
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.RealOverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.UpdateEndpoints
import com.sourceplusplus.protocol.artifact.endpoint.EndpointTableType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.PageType.*
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import kotlinx.browser.document
import kotlinx.html.*
import kotlinx.html.dom.append
import org.w3c.dom.Element

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RealOverviewPage(private val portalUuid: String) {

    init {
        console.log("Overview tab started")
        console.log("Connecting portal")
        eb.onopen = {
            js("portalConnected()")

            eb.registerHandler(UpdateEndpoints(portalUuid)) { error: String, message: Any ->
                displayEndpoints()
            }

            eb.publish(RealOverviewTabOpened, "{'portal_uuid': '$portalUuid'}")
        }
    }

    fun renderPage() {
        println("Rending Overview page")
        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""

        root.append {
            portalNav {
                navItem(REAL_OVERVIEW, isActive = true)
                navItem(OVERVIEW)
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

        displayEndpoints() //todo: remove
    }

    private fun updateTime(interval: QueryTimeFrame) {
    }

    fun displayEndpoints() {
        val root: Element = document.getElementById("endpoint_body_table")!!
        root.append {
            tr {
                td {
                    span {
                        i("far fa-globe") {
                            style = "font-size:1.5em;margin-right:5px"
                        }
                        +"test"
                    }
                }
                td {
                    +"test"
                }
                td {
                    +"test"
                }
                td {
                    +"test"
                }
                td {
                    +"test"
                }
            }
        }
    }
}
