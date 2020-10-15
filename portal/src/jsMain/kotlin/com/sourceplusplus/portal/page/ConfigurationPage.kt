package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.extensions.eb
import com.sourceplusplus.portal.extensions.jq
import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ConfigurationTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.UpdateArtifactAutoSubscribe
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.UpdateArtifactEntryMethod
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.DisplayArtifactConfiguration
import com.sourceplusplus.protocol.artifact.ArtifactConfigType.AUTO_SUBSCRIBE
import com.sourceplusplus.protocol.artifact.ArtifactConfigType.ENTRY_METHOD
import com.sourceplusplus.protocol.artifact.ArtifactInfoType.*
import com.sourceplusplus.protocol.artifact.ArtifactInformation
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.PageType.*
import kotlinx.browser.document
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
class ConfigurationPage(
    private val portalUuid: String,
    private val hideOverviewTab: Boolean = false
) {

    init {
        console.log("Configuration tab started")
        setupUI()
        
        @Suppress("EXPERIMENTAL_API_USAGE")
        eb.onopen = {
            js("portalConnected()")
            eb.registerHandler(DisplayArtifactConfiguration(portalUuid)) { _: String, message: dynamic ->
                updateArtifactConfigurationTable(Json.decodeFromDynamic(message.body))
            }
            eb.publish(ConfigurationTabOpened, json("portalUuid" to portalUuid))
        }
    }

    fun renderPage() {
        console.log("Rendering Configuration page")
        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""

        root.append {
            portalNav {
                navItem(OVERVIEW)
                navItem(TRACES) {
                    navSubItem(LATEST_TRACES, SLOWEST_TRACES, FAILED_TRACES)
                }
                navItem(CONFIGURATION, isActive = true)
            }
            configurationContent {
                navBar(false) {
                    rightAlign {
                        externalPortalButton()
                    }
                }
                configurationTable {
                    artifactConfiguration(ENTRY_METHOD, AUTO_SUBSCRIBE)
                    artifactInformation(QUALIFIED_NAME, CREATE_DATE, LAST_UPDATED, ENDPOINT)
                }
            }
        }
    }

    private fun updateArtifactConfigurationTable(artifact: ArtifactInformation) {
        jq("#artifact_qualified_name").text(artifact.artifactQualifiedName)
        jq("#artifact_create_date").text(moment.unix(artifact.createDate).format("LLLL"))
        jq("#artifact_last_updated").text(moment.unix(artifact.lastUpdated).format("LLLL"))

        if (artifact.config.endpoint) {
            jq("#entry_method_toggle").checkbox("set checked")
        } else {
            jq("#entry_method_toggle").checkbox("set unchecked")
        }

        if (artifact.config.subscribeAutomatically) {
            jq("#auto_subscribe_toggle").checkbox("set checked")
        } else {
            jq("#auto_subscribe_toggle").checkbox("set unchecked")
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
        eb.send(UpdateArtifactEntryMethod, json("portalUuid" to portalUuid, "entry_method" to entryMethod))
    }

    private fun toggledAutoSubscribe(autoSubscribe: Boolean) {
        eb.send(UpdateArtifactAutoSubscribe, json("portalUuid" to portalUuid, "auto_subscribe" to autoSubscribe))
    }
    
    private fun setupUI() {
        if (hideOverviewTab) {
            jq("#overview_link").css("display", "none")
            jq("#sidebar_overview_link").css("display", "none")
        }
        jq("#entry_method_toggle").change(fun(e: dynamic) {
            toggledEntryMethod(e.target.checked == true)
        })
        jq("#auto_subscribe_toggle").change(fun(e: dynamic) {
            toggledAutoSubscribe(e.target.checked == true)
        })
    }
}
