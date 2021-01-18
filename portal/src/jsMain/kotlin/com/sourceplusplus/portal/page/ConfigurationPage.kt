package com.sourceplusplus.portal.page

import com.bfergerson.vertx3.eventbus.EventBus
import com.sourceplusplus.portal.clickedTracesOrderType
import com.sourceplusplus.portal.clickedViewAsExternalPortal
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.extensions.toMoment
import com.sourceplusplus.portal.model.ArtifactConfigType.AUTO_SUBSCRIBE
import com.sourceplusplus.portal.model.ArtifactConfigType.ENTRY_METHOD
import com.sourceplusplus.portal.model.ArtifactInfoType.*
import com.sourceplusplus.portal.setCurrentPage
import com.sourceplusplus.protocol.portal.PageType.*
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.ConfigurationTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.UpdateArtifactAutoSubscribe
import com.sourceplusplus.protocol.ProtocolAddress.Global.UpdateArtifactEntryMethod
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayArtifactConfiguration
import com.sourceplusplus.protocol.artifact.ArtifactInformation
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.PortalConfiguration
import kotlinx.browser.document
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
) : IConfigurationPage(), PortalPage {

    override fun setupEventbus() {
        eb.registerHandler(DisplayArtifactConfiguration(portalUuid)) { _: dynamic, message: dynamic ->
            updateArtifactConfigurationTable(Json.decodeFromDynamic(message.body))
        }
        eb.publish(ConfigurationTabOpened, json("portalUuid" to portalUuid))
    }

    override fun renderPage(portalConfiguration: PortalConfiguration) {
        console.log("Rendering Configuration page")
        this.configuration = portalConfiguration

        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""
        root.append {
            portalNav {
                if (configuration.visibleOverview) navItem(OVERVIEW, onClick = {
                    setCurrentPage(eb, portalUuid, OVERVIEW)
                })
                if (configuration.visibleActivity) navItem(ACTIVITY, onClick = {
                    setCurrentPage(eb, portalUuid, ACTIVITY)
                })
                if (configuration.visibleTraces) navItem(TRACES, false, null) {
                    navSubItems(
                        PortalNavSubItem(LATEST_TRACES) { clickedTracesOrderType(eb, portalUuid, LATEST_TRACES) },
                        PortalNavSubItem(SLOWEST_TRACES) { clickedTracesOrderType(eb, portalUuid, SLOWEST_TRACES) },
                        PortalNavSubItem(FAILED_TRACES) { clickedTracesOrderType(eb, portalUuid, FAILED_TRACES) }
                    )
                }
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
            jq("#artifact_endpoint").text("true")
        } else {
            jq("#artifact_endpoint").text("false")
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
