package com.sourceplusplus.plugin.coordinate.artifact.track

import com.sourceplusplus.api.model.config.SourcePluginConfig
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

import java.util.concurrent.TimeUnit

/**
 * Keeps track of all artifact subscriptions.
 * Distributes specific artifact subscriptions to specified trackers.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginArtifactSubscriptionTracker extends AbstractVerticle {

    public static final String SYNC_AUTOMATIC_SUBSCRIPTIONS = "SyncAutomaticSubscriptions"

    @Override
    void start() throws Exception {
        //keep subscriptions alive
        vertx.setPeriodic(TimeUnit.MINUTES.toMillis(2), {
            if (SourcePluginConfig.current.activeEnvironment?.appUuid) {
                SourcePluginConfig.current.activeEnvironment.coreClient.refreshSubscriberApplicationSubscriptions(
                        SourcePluginConfig.current.activeEnvironment.appUuid, {
                    if (it.succeeded()) {
                        log.debug("Refreshed subscriptions")
                    } else {
                        log.error("Failed to refresh subscriptions", it.cause())
                    }
                })
            }
        })
        log.info("{} started", getClass().getSimpleName())
    }
}
