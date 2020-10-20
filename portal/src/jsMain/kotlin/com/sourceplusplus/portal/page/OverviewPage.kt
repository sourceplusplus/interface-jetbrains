package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.clickedViewAsExternalPortal
import com.sourceplusplus.portal.model.EndpointTableType
import com.sourceplusplus.portal.model.PageType.*
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.OverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Portal.UpdateEndpoints
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.endpoint.EndpointResult
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.PortalConfiguration
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
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class OverviewPage(
    override val portalUuid: String,
    private val eb: EventBus
) : IOverviewPage {

    private lateinit var configuration: PortalConfiguration

    override fun setupEventbus() {
        eb.registerHandler(UpdateEndpoints(portalUuid)) { _: dynamic, message: dynamic ->
            displayEndpoints(Json.decodeFromDynamic(message.body))
        }
        eb.publish(OverviewTabOpened, json("portalUuid" to portalUuid))
    }

    override fun renderPage(portalConfiguration: PortalConfiguration) {
        println("Rending Overview page")
        this.configuration = portalConfiguration

        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""
        root.append {
            portalNav {
                if (configuration.visibleOverview) navItem(OVERVIEW, isActive = true)
                if (configuration.visibleActivity) navItem(ACTIVITY)
                if (configuration.visibleTraces) navItem(TRACES) {
                    navSubItem(LATEST_TRACES, SLOWEST_TRACES, FAILED_TRACES)
                }
                if (configuration.visibleConfiguration) navItem(CONFIGURATION)
            }
            pusherContent {
                navBar {
                    timeDropdown(*QueryTimeFrame.values()) { updateTime(it) }
                    //calendar()

                    rightAlign {
                        externalPortalButton { clickedViewAsExternalPortal(eb) }
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
