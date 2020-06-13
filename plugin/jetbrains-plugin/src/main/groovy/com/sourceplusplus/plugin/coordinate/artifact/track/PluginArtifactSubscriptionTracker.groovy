package com.sourceplusplus.plugin.coordinate.artifact.track

import com.google.common.collect.Sets
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle

import java.util.concurrent.TimeUnit

/**
 * Keeps track of all artifact subscriptions.
 * Distributes specific artifact subscriptions to specified trackers.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginArtifactSubscriptionTracker extends AbstractVerticle {

    public static final String SYNC_AUTOMATIC_SUBSCRIPTIONS = "SyncAutomaticSubscriptions"

    //todo: properly refresh when app uuid changes
    private static final Set<String> PENDING_DATA_AVAILABLE = Sets.newConcurrentHashSet()

    @Override
    void start() throws Exception {
        //keep subscriptions alive
        vertx.setPeriodic(TimeUnit.MINUTES.toMillis(5), {
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

        //refresh markers when they become available
        vertx.eventBus().consumer(IntelliJSourceMark.SOURCE_MARK_CREATED, {
            def sourceMark = it.body() as IntelliJSourceMark

            //pending data available/subscriptions
            if (PENDING_DATA_AVAILABLE.remove(sourceMark.artifactQualifiedName)) {
                sourceMark.markArtifactDataAvailable()
            }
        })

        //artifact has data
        //todo: could watch ARTIFACT_TRACE_UPDATED too
        vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
            def artifactMetricResult = it.body() as ArtifactMetricResult
            if (artifactMetricResult.appUuid() == SourcePluginConfig.current.activeEnvironment.appUuid) {
                def sourceMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                        artifactMetricResult.artifactQualifiedName()) as IntelliJSourceMark
                if (sourceMark != null) {
                    sourceMark.markArtifactDataAvailable()
                    PENDING_DATA_AVAILABLE.remove(artifactMetricResult.artifactQualifiedName())
                } else {
                    PENDING_DATA_AVAILABLE.add(artifactMetricResult.artifactQualifiedName())
                }
            }
        })
        log.info("{} started", getClass().getSimpleName())
    }
}
