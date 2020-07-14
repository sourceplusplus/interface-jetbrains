package com.sourceplusplus.plugin.coordinate.integration

import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.integration.IntegrationInfo
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.INTEGRATION_INFO_UPDATED

/**
 * Keeps track of integrations which are currently enabled on Source++ Core.
 *
 * @version 0.3.2
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntegrationInfoTracker extends AbstractVerticle {

    private static final Map<String, IntegrationInfo> ACTIVE_INTEGRATIONS = new HashMap<>()

    static IntegrationInfo getActiveIntegrationInfo(String id) {
        return ACTIVE_INTEGRATIONS.get(id)
    }

    @Override
    void start() throws Exception {
        syncIntegrationInfos()

        vertx.eventBus().consumer(INTEGRATION_INFO_UPDATED.address, {
            def integration = it.body() as IntegrationInfo
            if (integration.enabled()) {
                ACTIVE_INTEGRATIONS.put(integration.id(), integration)
            } else {
                ACTIVE_INTEGRATIONS.remove(integration.id())
            }
            SourceMarkerPlugin.INSTANCE.refreshAvailableSourceFileMarkers(true)
        })
    }

    private static void syncIntegrationInfos() {
        SourcePluginConfig.current.activeEnvironment.coreClient.info({
            if (it.succeeded()) {
                it.result().activeIntegrations().each {
                    ACTIVE_INTEGRATIONS.put(it.id(), it)
                }
            } else {
                log.error("Failed to get core info", it.cause())
            }
        })
    }
}