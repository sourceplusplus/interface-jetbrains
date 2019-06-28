package com.sourceplusplus.plugin.coordinate.artifact.track

import com.google.common.collect.Sets
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.artifact.ArtifactSubscribeRequest
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.marker.mark.GutterMark
import io.vertx.core.AbstractVerticle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

import static com.sourceplusplus.plugin.PluginBootstrap.sourcePlugin

/**
 * Keeps track of all artifact subscriptions.
 * Distributes specific artifact subscriptions to specified trackers.
 *
 * @version 0.2.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class PluginArtifactSubscriptionTracker extends AbstractVerticle {

    public static final String SUBSCRIBE_TO_ARTIFACT = "SubscribeToArtifact"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT = "UnsubscribeFromArtifact"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static final Set<String> PENDING_DATA_AVAILABLE = Sets.newConcurrentHashSet()
    private static final Set<String> PENDING_SUBSCRIBED = Sets.newConcurrentHashSet()

    @Override
    void start() throws Exception {
        //todo: change to just subscriber
        //subscribe to automatic subscriptions
        SourcePluginConfig.current.activeEnvironment.coreClient.getApplicationSubscriptions(
                SourcePluginConfig.current.activeEnvironment.appUuid, true, {
            if (it.succeeded()) {
                it.result().each {
                    if (it.automaticSubscription()) {
                        PENDING_SUBSCRIBED.add(it.artifactQualifiedName())
                    }
                }
            } else {
                log.error("Failed to get application subscriptions", it.cause())
            }
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
        vertx.eventBus().consumer(SourcePlugin.SOURCE_FILE_MARKER_ACTIVATED, {
            def qualifiedClassName = it.body() as String

            //current subscriptions
            SourcePluginConfig.current.activeEnvironment.coreClient.getApplicationSubscriptions(
                    SourcePluginConfig.current.activeEnvironment.appUuid, false, {
                if (it.succeeded()) {
                    it.result().findAll { it.artifactQualifiedName().startsWith(qualifiedClassName) }.each {
                        if (SourcePluginConfig.current.methodGutterMarksEnabled) {
                            def gutterMark = sourcePlugin.getSourceMark(it.artifactQualifiedName()) as GutterMark
                            gutterMark?.markArtifactSubscribed()
                        }
                    }
                } else {
                    log.error("Failed to get application subscriptions", it.cause())
                }
            })

            //pending data available/subscriptions
            PENDING_DATA_AVAILABLE.findAll { it.startsWith(qualifiedClassName) }.each {
                if (SourcePluginConfig.current.methodGutterMarksEnabled) {
                    def gutterMark = sourcePlugin.getSourceMark(it) as GutterMark
                    if (gutterMark != null) {
                        gutterMark.markArtifactHasData()
                        PENDING_DATA_AVAILABLE.remove(it)
                    }
                }
            }
            PENDING_SUBSCRIBED.findAll { it.startsWith(qualifiedClassName) }.each {
                if (SourcePluginConfig.current.methodGutterMarksEnabled) {
                    def gutterMark = sourcePlugin.getSourceMark(it) as GutterMark
                    if (gutterMark != null) {
                        gutterMark.markArtifactSubscribed()
                        PENDING_SUBSCRIBED.remove(it)
                    }
                }
            }
        })

        //artifact has data
        vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
            def artifactMetricResult = it.body() as ArtifactMetricResult
            if (SourcePluginConfig.current.methodGutterMarksEnabled
                    && artifactMetricResult.appUuid() == SourcePluginConfig.current.activeEnvironment.appUuid) {
                def gutterMark = sourcePlugin.getSourceMark(
                        artifactMetricResult.artifactQualifiedName()) as GutterMark
                if (gutterMark != null) {
                    gutterMark.markArtifactHasData()
                    PENDING_DATA_AVAILABLE.remove(artifactMetricResult.artifactQualifiedName())
                } else {
                    PENDING_DATA_AVAILABLE.add(artifactMetricResult.artifactQualifiedName())
                }
            }
        })

        //subscribe to artifact
        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT, { resp ->
            def request = resp.body() as ArtifactSubscribeRequest
            log.info("Sending artifact subscription request: " + request)

            SourcePluginConfig.current.activeEnvironment.coreClient.subscribeToArtifact(request, {
                if (it.succeeded()) {
                    resp.reply(request)

                    def gutterMark = sourcePlugin.getSourceMark(request.artifactQualifiedName()) as GutterMark
                    if (gutterMark != null) {
                        gutterMark.markArtifactSubscribed()
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
            def request = resp.body() as SourceArtifactUnsubscribeRequest
            log.info("Sending artifact unsubscription request: " + request)

            SourcePluginConfig.current.activeEnvironment.coreClient.unsubscribeFromArtifact(request, {
                if (it.succeeded()) {
                    resp.reply(request)
                    PENDING_SUBSCRIBED.remove(request.artifactQualifiedName())

                    def gutterMark = sourcePlugin.getSourceMark(request.artifactQualifiedName()) as GutterMark
                    gutterMark?.markArtifactUnsubscribed()
                } else {
                    it.cause().printStackTrace()
                    resp.fail(500, it.cause().message)
                }
            })
        })
        log.info("{} started", getClass().getSimpleName())
    }
}
