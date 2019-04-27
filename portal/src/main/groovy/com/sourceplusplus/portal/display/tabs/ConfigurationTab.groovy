package com.sourceplusplus.portal.display.tabs

import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.display.PortalTab
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Used to display and configure a given source code artifact.
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ConfigurationTab extends AbstractTab {

    public static final String CONFIGURATION_TAB_OPENED = "ConfigurationTabOpened"
    public static final String DISPLAY_ARTIFACT_CONFIGURATION = "DisplayArtifactConfiguration"
    public static final String UPDATE_ARTIFACT_FORCE_SUBSCRIBE = "UpdateArtifactForceSubscribe"

    private static final Logger log = LoggerFactory.getLogger(this.name)

    private final SourceCoreClient coreClient
    private final boolean pluginAvailable
    private boolean updateConfigurationPermitted

    ConfigurationTab(SourceCoreClient coreClient, boolean pluginAvailable) {
        super(PortalTab.Configuration)
        this.coreClient = Objects.requireNonNull(coreClient)
        this.pluginAvailable = pluginAvailable
    }

    @Override
    void start() throws Exception {
        super.start()
        updateConfigurationPermitted = pluginAvailable ||
                config().getJsonObject("permissions").getBoolean("update_artifact_configuration")

        vertx.eventBus().consumer(CONFIGURATION_TAB_OPENED, {
            log.info("Configuration tab opened")
            def message = JsonObject.mapFrom(it.body())
            def portal = SourcePortal.getPortal(message.getString("portal_uuid"))
            portal.interface.currentTab = PortalTab.Configuration
            updateUI(portal)
        })

        vertx.eventBus().consumer(UPDATE_ARTIFACT_FORCE_SUBSCRIBE, {
            log.info("Updating artifact force subscribe")
            def request = JsonObject.mapFrom(it.body())
            def portal = SourcePortal.getPortal(request.getString("portal_uuid"))
            def config = SourceArtifactConfig.builder()
                    .forceSubscribe(request.getBoolean("force_subscribe"))
                    .build()
            coreClient.createOrUpdateArtifactConfig(portal.appUuid, portal.interface.viewingPortalArtifact, config, {
                if (it.succeeded()) {
                    SourcePortal.getSimilarPortals(portal).each {
                        updateUI(it)
                    }
                } else {
                    log.error("Failed to update artifact config: " + portal.interface.viewingPortalArtifact, it.cause())
                }
            })
        })
    }

    @Override
    void updateUI(SourcePortal portal) {
        coreClient.getArtifact(portal.appUuid, portal.interface.viewingPortalArtifact, {
            if (it.succeeded()) {
                vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_ARTIFACT_CONFIGURATION",
                        new JsonObject(Json.encode(it.result())))
            } else {
                log.error("Failed to get artifact: " + portal.interface.viewingPortalArtifact, it.cause())
            }
        })
    }
}
