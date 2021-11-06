package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import spp.protocol.ProtocolAddress.Global.UpdateArtifactAutoSubscribe
import spp.protocol.ProtocolAddress.Global.UpdateArtifactEntryMethod
import spp.protocol.portal.PageType
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * Used to display and configure a given source code artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ConfigurationDisplay(
    private val refreshIntervalMs: Int,
    private val pluginAvailable: Boolean
) : AbstractDisplay(PageType.CONFIGURATION) {

    companion object {
        private val log = LoggerFactory.getLogger(ConfigurationDisplay::class.java)
    }

    private var updateConfigurationPermitted: Boolean = false

    override suspend fun start() {
        updateConfigurationPermitted = pluginAvailable

//        vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.address, {
//            log.debug("Artifact configuration updated")
//            def artifact = it.body() as SourceArtifact
//            SourcePortal.getPortals(artifact.appUuid(), artifact.artifactQualifiedName()).each {
//                cacheAndDisplayArtifactConfiguration(it, artifact)
//            }
//        })

        vertx.eventBus().consumer<JsonObject>(UpdateArtifactEntryMethod) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
            if (!updateConfigurationPermitted) {
                log.warn("Rejected artifact entry method update")
                updateUI(portal)
                return@consumer
            }

//            log.info("Updating artifact entry method")
//            val config = SourceArtifactConfig.builder()
//                    .endpoint(request.getBoolean("entry_method"))
//                    .build()
//            SourcePortalConfig.current.getCoreClient(portal.appUuid).createOrUpdateArtifactConfig(
//                    portal.appUuid, portal.portalUI.viewingPortalArtifact, config, {
//                if (it.succeeded()) {
//                    log.info("Successfully updated artifact entry method")
//                } else {
//                    log.error("Failed to update artifact config: " + portal.portalUI.viewingPortalArtifact, it.cause())
//                }
//            })
        }
        vertx.eventBus().consumer<JsonObject>(UpdateArtifactAutoSubscribe) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
            if (!updateConfigurationPermitted) {
                log.warn("Rejected artifact auto subscribe update")
                updateUI(portal)
                return@consumer
            }

//            log.info("Updating artifact auto subscribe")
//            def config = SourceArtifactConfig.builder()
//                    .subscribeAutomatically(request.getBoolean("auto_subscribe"))
//                    .build()
//            SourcePortalConfig.current.getCoreClient(portal.appUuid).createOrUpdateArtifactConfig(
//                    portal.appUuid, portal.portalUI.viewingPortalArtifact, config, {
//                if (it.succeeded()) {
//                    log.info("Successfully updated artifact auto subscribe")
//                } else {
//                    log.error("Failed to update artifact config: " + portal.portalUI.viewingPortalArtifact, it.cause())
//                }
//            })
        }

        super.start()
    }

    override fun updateUI(portal: SourcePortal) {
        if (portal.configuration.currentPage != thisTab) {
            return
        }

        if (portal.configurationView.artifact != null) {
            //display cached
            cacheAndDisplayArtifactConfiguration(portal, portal.configurationView.artifact!!)
        }
        if (!pluginAvailable || portal.configurationView.artifact == null) {
            //fetch latest
//            SourcePortalConfig.current.getCoreClient(portal.appUuid).getArtifact(
//                    portal.appUuid, portal.portalUI.viewingPortalArtifact, {
//                if (it.succeeded()) {
//                    cacheAndDisplayArtifactConfiguration(portal, it.result())
//                } else {
//                    log.error("Failed to get artifact: " + portal.portalUI.viewingPortalArtifact, it.cause())
//                }
//            })
        }
    }

    fun cacheAndDisplayArtifactConfiguration(portal: SourcePortal, artifact: JsonObject) {
        portal.configurationView.artifact = artifact
        if (portal.configuration.currentPage == thisTab) {
//            vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_ARTIFACT_CONFIGURATION", JsonObject(Json.encode(
//                    artifact.withArtifactQualifiedName(getShortQualifiedFunctionName(artifact.artifactQualifiedName())))))
        }
    }
}
