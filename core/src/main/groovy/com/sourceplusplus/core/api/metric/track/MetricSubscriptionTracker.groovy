package com.sourceplusplus.core.api.metric.track

import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.metric.ArtifactMetricQuery
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import groovy.util.logging.Slf4j
import io.vertx.core.CompositeFuture
import io.vertx.core.Promise

import java.time.Instant
import java.time.temporal.ChronoUnit

import static com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType.METRICS
import static com.sourceplusplus.api.util.ArtifactNameUtils.getShortQualifiedFunctionName

/**
 * Keeps track of artifact metric subscriptions.
 *
 * @version 0.3.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class MetricSubscriptionTracker extends ArtifactSubscriptionTracker {

    public static final String PUBLISH_ARTIFACT_METRICS = "PublishArtifactMetrics"
    public static final String SUBSCRIBE_TO_ARTIFACT_METRICS = "SubscribeToArtifactMetrics"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT_METRICS = "UnsubscribeFromArtifactMetrics"

    MetricSubscriptionTracker(SourceCore core) {
        super(core)
    }

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(PUBLISH_ARTIFACT_METRICS, {
            publishLatestMetrics(it.body() as ArtifactMetricSubscribeRequest)
        })

        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT_METRICS, { request ->
            def subRequest = request.body() as ArtifactMetricSubscribeRequest

            core.storage.getSubscriberArtifactSubscriptions(subRequest.subscriberUuid(),
                    subRequest.appUuid(), subRequest.artifactQualifiedName(), {
                if (it.succeeded()) {
                    boolean createSubscription = true
                    def futures = []
                    it.result().findAll { it.type == METRICS }.each {
                        def currentSubscription = it as ArtifactMetricSubscribeRequest
                        if (subRequest.timeFrame() == currentSubscription.timeFrame()) {
                            if (subRequest != currentSubscription) {
                                def promise = Promise.promise()
                                futures.add(promise)

                                subRequest = currentSubscription.withSubscribeDate(Instant.now())
                                        .withMetricTypes(currentSubscription.metricTypes() + subRequest.metricTypes())
                                core.storage.updateArtifactSubscription(currentSubscription, subRequest, promise)
                            }
                            createSubscription = false
                        }
                    }
                    if (createSubscription) {
                        def promise = Promise.promise()
                        futures.add(promise)

                        subRequest = subRequest.withSubscribeDate(Instant.now())
                        core.storage.createArtifactSubscription(subRequest, promise)
                    }

                    CompositeFuture.all(futures).onComplete({
                        if (it.succeeded()) {
                            request.reply(true)
                            publishLatestMetrics(subRequest)
                        } else {
                            log.error("Failed to add artifact subscription", it.cause())
                            request.fail(500, it.cause().message)
                        }
                    })
                } else {
                    log.error("Failed to get artifact subscriptions", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        })
        vertx.eventBus().consumer(UNSUBSCRIBE_FROM_ARTIFACT_METRICS, { request ->
            def unsubRequest = request.body() as ArtifactMetricUnsubscribeRequest
            core.storage.getSubscriberArtifactSubscriptions(unsubRequest.subscriberUuid(),
                    unsubRequest.appUuid(), unsubRequest.artifactQualifiedName(), {
                if (it.succeeded()) {
                    if (!it.result().isEmpty()) {
                        if (unsubRequest.removeAllArtifactMetricSubscriptions()) {
                            core.storage.deleteSubscriberArtifactSubscriptions(unsubRequest.subscriberUuid(),
                                    unsubRequest.appUuid(), unsubRequest.artifactQualifiedName(), unsubRequest.type, {
                                if (it.succeeded()) {
                                    request.reply(true)
                                } else {
                                    log.error("Failed to delete artifact subscription", it.cause())
                                    request.fail(500, it.cause().message)
                                }
                            })
                        } else {
                            def metricSubscriptions = it.result()
                                    .findAll { it.type == METRICS } as List<ArtifactMetricSubscribeRequest>
                            def updatedMetricSubscriptions = new HashMap<ArtifactMetricSubscribeRequest, ArtifactMetricSubscribeRequest>()
                            metricSubscriptions.each {
                                if (unsubRequest.removeTimeFrames().contains(it.timeFrame())) {
                                    updatedMetricSubscriptions.put(it, null)
                                } else if (unsubRequest.removeTimeFramedMetricTypes().intersect(it.asTimeFramedMetricTypes())) {
                                    def intersection = unsubRequest.removeTimeFramedMetricTypes()
                                            .find { it.timeFrame() == it.timeFrame() }

                                    def updatedMetricTypes = it.metricTypes() - intersection.metricType()
                                    if (updatedMetricTypes.isEmpty()) {
                                        updatedMetricSubscriptions.put(it, null)
                                    } else {
                                        updatedMetricSubscriptions.put(it, it.withMetricTypes(updatedMetricTypes))
                                    }
                                } else {
                                    def updatedMetricTypes = it.metricTypes() - unsubRequest.removeMetricTypes()
                                    if (updatedMetricTypes.isEmpty()) {
                                        updatedMetricSubscriptions.put(it, null)
                                    } else {
                                        updatedMetricSubscriptions.put(it, it.withMetricTypes(updatedMetricTypes))
                                    }
                                }
                            }

                            if (updatedMetricSubscriptions.isEmpty()) {
                                core.storage.deleteSubscriberArtifactSubscriptions(unsubRequest.subscriberUuid(),
                                        unsubRequest.appUuid(), unsubRequest.artifactQualifiedName(), unsubRequest.type, {
                                    if (it.succeeded()) {
                                        request.reply(true)
                                    } else {
                                        log.error("Failed to delete artifact subscription", it.cause())
                                        request.fail(500, it.cause().message)
                                    }
                                })
                            } else {
                                def futures = []
                                updatedMetricSubscriptions.each {
                                    def promise = Promise.promise()
                                    futures.add(promise)
                                    if (it.value == null) {
                                        core.storage.deleteArtifactSubscription(it.key, promise)
                                    } else {
                                        core.storage.updateArtifactSubscription(it.key, it.value, promise)
                                    }
                                }
                                CompositeFuture.all(futures).onComplete({
                                    if (it.succeeded()) {
                                        request.reply(true)
                                    } else {
                                        log.error("Failed to unsubscribe from artifact metrics", it.cause())
                                        request.fail(500, it.cause().message)
                                    }
                                })
                            }
                        }
                    } else {
                        request.reply(true)
                    }
                } else {
                    log.error("Failed to unsubscribe from artifact metrics", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void publishLatestMetrics(ArtifactMetricSubscribeRequest subscription) {
        def metricQuery = ArtifactMetricQuery.builder()
                .appUuid(subscription.appUuid())
                .artifactQualifiedName(subscription.artifactQualifiedName())
                .metricTypes(subscription.metricTypes())
                .timeFrame(subscription.timeFrame()) //todo: why is time frame and start/stop needed?
                .start(Instant.now().minus(subscription.timeFrame().minutes, ChronoUnit.MINUTES))
                .stop(Instant.now())
                .step("MINUTE")
                .build()
        core.metricAPI.getArtifactMetrics(metricQuery, {
            if (it.succeeded()) {
                def metricResult = it.result()
                vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, metricResult)
                log.debug("Published updated metrics for artifact: " + getShortQualifiedFunctionName(metricResult.artifactQualifiedName()))
            } else {
                it.cause().printStackTrace()
            }
        })
    }
}
