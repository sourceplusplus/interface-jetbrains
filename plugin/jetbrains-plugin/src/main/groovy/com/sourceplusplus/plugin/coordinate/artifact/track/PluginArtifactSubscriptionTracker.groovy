package com.sourceplusplus.plugin.coordinate.artifact.track

import com.google.common.collect.Sets
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.artifact.ArtifactSubscribeRequest
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJSourceMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMark
import com.sourceplusplus.plugin.intellij.portal.IntelliJSourcePortal
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin

import java.util.concurrent.TimeUnit

/**
 * Keeps track of all artifact subscriptions.
 * Distributes specific artifact subscriptions to specified trackers.
 *
 * @version 0.2.5
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PluginArtifactSubscriptionTracker extends AbstractVerticle {

    public static final String SYNC_AUTOMATIC_SUBSCRIPTIONS = "SyncAutomaticSubscriptions"
    public static final String SUBSCRIBE_TO_ARTIFACT = "SubscribeToArtifact"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT = "UnsubscribeFromArtifact"

    private static final Set<String> PENDING_DATA_AVAILABLE = Sets.newConcurrentHashSet()
    private static final Set<String> PENDING_SUBSCRIBED = Sets.newConcurrentHashSet()

    @Override
    void start() throws Exception {
        //subscribe to automatic subscriptions
        syncAutomaticSubscriptions()
        vertx.eventBus().consumer(SYNC_AUTOMATIC_SUBSCRIPTIONS, {
            syncAutomaticSubscriptions()
        })

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
            if (PENDING_SUBSCRIBED.remove(sourceMark.artifactQualifiedName)) {
                sourceMark.markArtifactSubscribed()
            }
        })

        //artifact has data
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

        //subscribe to artifact
        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT, { resp ->
            ArtifactSubscribeRequest request = resp.body() as ArtifactSubscribeRequest
            log.info("Sending artifact subscription request: " + request)

            SourcePluginConfig.current.activeEnvironment.coreClient.subscribeToArtifact(request, {
                if (it.succeeded()) {
                    resp.reply(request)

                    def sourceMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                            request.artifactQualifiedName()) as IntelliJSourceMark
                    if (sourceMark != null) {
                        sourceMark.markArtifactSubscribed()
                        PENDING_SUBSCRIBED.remove(request.artifactQualifiedName())
                    } else {
                        PENDING_SUBSCRIBED.add(request.artifactQualifiedName())
                    }
                } else {
                    it.cause().printStackTrace()
                    resp.fail(500, it.cause().message)
                }
            })
        })
        //unsubscribe from artifact
        vertx.eventBus().consumer(UNSUBSCRIBE_FROM_ARTIFACT, { resp ->
            SourceArtifactUnsubscribeRequest request = resp.body() as SourceArtifactUnsubscribeRequest
            log.info("Sending artifact unsubscription request: " + request)

            SourcePluginConfig.current.activeEnvironment.coreClient.unsubscribeFromArtifact(request, {
                if (it.succeeded()) {
                    resp.reply(request)
                    PENDING_SUBSCRIBED.remove(request.artifactQualifiedName())

                    def gutterMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                            request.artifactQualifiedName()) as IntelliJGutterMark
                    if (gutterMark) {
                        gutterMark.markArtifactUnsubscribed()
                        if (gutterMark.portalRegistered) {
                            IntelliJSourcePortal.getPortal(gutterMark.portalUuid)?.close()
                        }
                    }
                } else {
                    it.cause().printStackTrace()
                    resp.fail(500, it.cause().message)
                }
            })
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private static void syncAutomaticSubscriptions() {
        //todo: ignore calls which start while this method is already running
        //todo: change to just subscriber
        SourcePluginConfig.current.activeEnvironment.coreClient.getApplicationSubscriptions(
                SourcePluginConfig.current.activeEnvironment.appUuid, true, {
            if (it.succeeded()) {
                it.result().each {
                    if (it.automaticSubscription() || it.forceSubscription()) {
                        def sourceMark = SourceMarkerPlugin.INSTANCE.getSourceMark(
                                it.artifactQualifiedName()) as IntelliJSourceMark
                        if (sourceMark != null) {
                            sourceMark.markArtifactSubscribed()
                        } else {
                            PENDING_SUBSCRIBED.add(it.artifactQualifiedName())
                        }
                    }
                }
            } else {
                log.error("Failed to get application subscriptions", it.cause())
            }
        })
    }
}
