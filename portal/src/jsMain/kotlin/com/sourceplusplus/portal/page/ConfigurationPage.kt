package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.PortalBundle.translate
import com.sourceplusplus.portal.clickedLogsOrderType
import com.sourceplusplus.portal.clickedTracesOrderType
import com.sourceplusplus.portal.clickedViewAsExternalPortal
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.toMoment
import com.sourceplusplus.portal.model.ArtifactConfigType.AUTO_SUBSCRIBE
import com.sourceplusplus.portal.model.ArtifactConfigType.ENTRY_METHOD
import com.sourceplusplus.portal.model.ArtifactInfoType.*
import com.sourceplusplus.portal.setCurrentPage
import com.sourceplusplus.portal.template.*
import spp.protocol.ProtocolAddress.Global.RefreshPortal
import spp.protocol.ProtocolAddress.Global.UpdateArtifactAutoSubscribe
import spp.protocol.ProtocolAddress.Global.UpdateArtifactEntryMethod
import spp.protocol.ProtocolAddress.Portal.DisplayArtifactConfiguration
import spp.protocol.artifact.ArtifactInformation
import spp.protocol.artifact.log.LogOrderType.NEWEST_LOGS
import spp.protocol.artifact.trace.TraceOrderType.*
import spp.protocol.portal.PageType.*
import spp.protocol.portal.PortalConfiguration
import kotlinx.browser.document
import kotlinx.dom.removeClass
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
class ConfigurationPage(
    override val portalUuid: String,
    private val eb: EventBus
) : IConfigurationPage() {

    override fun setupEventbus() {
        if (!setup) {
            setup = true
            eb.registerHandler(DisplayArtifactConfiguration(portalUuid)) { _: dynamic, message: dynamic ->
                updateArtifactConfigurationTable(Json.decodeFromDynamic(message.body))
            }
        }
        eb.publish(RefreshPortal, portalUuid)
    }

    override fun renderPage(portalConfiguration: PortalConfiguration) {
        console.log("Rendering Configuration page")
        this.configuration = portalConfiguration

        document.title = translate("Configuration - SourceMarker")
        val root: Element = document.getElementById("root")!!
        root.removeClass("overflow_y_hidden")
        root.innerHTML = ""
        root.append {
            portalNav {
                if (configuration.visibleOverview) navItem(OVERVIEW, onClick = {
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
                if (configuration.visibleConfiguration) navItem(CONFIGURATION, isActive = true, onClick = {
                    setCurrentPage(eb, portalUuid, CONFIGURATION)
                })
            }
            configurationContent {
                navBar(false) {
                    rightAlign {
                        externalPortalButton { clickedViewAsExternalPortal(eb, portalUuid) }
                    }
                }
                configurationTable {
                    artifactConfiguration(ENTRY_METHOD, AUTO_SUBSCRIBE)
                    artifactInformation(QUALIFIED_NAME, CREATE_DATE, LAST_UPDATED, ENDPOINT)
                }
            }
        }

        setupUI()
    }

    private fun updateArtifactConfigurationTable(artifact: ArtifactInformation) {
        jq("#artifact_qualified_name").text(artifact.artifactQualifiedName)
        jq("#artifact_create_date").text(artifact.createDate.toMoment().format("LLLL"))
        jq("#artifact_last_updated").text(artifact.lastUpdated.toMoment().format("LLLL"))

        if (artifact.config.endpoint) {
            js("\$('#entry_method_toggle').checkbox(\"set checked\");")
        } else {
            js("\$('#entry_method_toggle').checkbox(\"set unchecked\");")
        }

        if (artifact.config.subscribeAutomatically) {
            js("\$('#auto_subscribe_toggle').checkbox(\"set checked\");")
        } else {
            js("\$('#auto_subscribe_toggle').checkbox(\"set unchecked\");")
        }

        if (!artifact.config.endpointName.isBlank()) {
            jq("#artifact_endpoint").text(artifact.config.endpointName)
        } else if (!artifact.config.endpointIds.isNullOrEmpty()) {
            jq("#artifact_endpoint").text(translate("true"))
        } else {
            jq("#artifact_endpoint").text(translate("false"))
        }
    }

    private fun toggledEntryMethod(entryMethod: Boolean) {
        eb.send(UpdateArtifactEntryMethod, json("portalUuid" to portalUuid, "entryMethod" to entryMethod))
    }

    private fun toggledAutoSubscribe(autoSubscribe: Boolean) {
        eb.send(UpdateArtifactAutoSubscribe, json("portalUuid" to portalUuid, "autoSubscribe" to autoSubscribe))
    }

    private fun setupUI() {
        if (!configuration.visibleActivity) {
            jq("#activity_link").css("display", "none")
            jq("#sidebar_activity_link").css("display", "none")
        }
        jq("#entry_method_toggle").change(fun(e: dynamic) {
            toggledEntryMethod(e.target.checked == true)
        })
        jq("#auto_subscribe_toggle").change(fun(e: dynamic) {
            toggledAutoSubscribe(e.target.checked == true)
        })
    }
}
