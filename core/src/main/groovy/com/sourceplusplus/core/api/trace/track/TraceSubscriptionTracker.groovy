package com.sourceplusplus.core.api.trace.track

import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import groovy.util.logging.Slf4j
import io.vertx.core.CompositeFuture
import io.vertx.core.Promise

import java.time.Instant
import java.time.temporal.ChronoUnit

import static com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType.TRACES

/**
 * Keeps track of artifact trace subscriptions.
 *
 * @version 0.3.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class TraceSubscriptionTracker extends ArtifactSubscriptionTracker {

    public static final String PUBLISH_ARTIFACT_TRACES = "PublishArtifactTraces"
    public static final String SUBSCRIBE_TO_ARTIFACT_TRACES = "SubscribeToArtifactTraces"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT_TRACES = "UnsubscribeFromArtifactTraces"

    TraceSubscriptionTracker(SourceCore core) {
        super(core)
    }

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(PUBLISH_ARTIFACT_TRACES, {
            def subscription = it.body() as ArtifactTraceSubscribeRequest
            subscription.orderTypes().each {
                switch (it) { //todo: add time frame to publish params
                    case TraceOrderType.LATEST_TRACES:
                        publishLatestTraces(subscription)
                        break
                    case TraceOrderType.SLOWEST_TRACES:
                        publishSlowestTraces(subscription)
                        break
                    case TraceOrderType.FAILED_TRACES:
                        publishFailedTraces(subscription)
                        break
                }
            }
        })

        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT_TRACES, { request ->
            def newSubscription = request.body() as ArtifactTraceSubscribeRequest
            core.storage.getSubscriberArtifactSubscriptions(newSubscription.subscriberUuid(),
                    newSubscription.appUuid(), newSubscription.artifactQualifiedName(), {
                if (it.succeeded()) {
                    boolean createSubscription = true
                    def futures = []
                    it.result().findAll { it.type == TRACES }.each {
                        def currentSubscription = it as ArtifactTraceSubscribeRequest
                        if (newSubscription.timeFrame() == currentSubscription.timeFrame()) {
                            if (newSubscription != currentSubscription) {
                                def promise = Promise.promise()
                                futures.add(promise)

                                newSubscription = currentSubscription.withSubscribeDate(Instant.now())
                                        .withOrderTypes(currentSubscription.orderTypes() + newSubscription.orderTypes())
                                core.storage.updateArtifactSubscription(currentSubscription, newSubscription, promise)
                            }
                            createSubscription = false
                        }
                    }
                    if (createSubscription) {
                        def promise = Promise.promise()
                        futures.add(promise)

                        newSubscription = newSubscription.withSubscribeDate(Instant.now())
                        core.storage.createArtifactSubscription(newSubscription, promise)
                    }

                    CompositeFuture.all(futures).onComplete({
                        if (it.succeeded()) {
                            request.reply(true)

                            newSubscription.orderTypes().each {
                                switch (it) {
                                    case TraceOrderType.LATEST_TRACES:
                                        publishLatestTraces(newSubscription)
                                        break
                                    case TraceOrderType.SLOWEST_TRACES:
                                        publishSlowestTraces(newSubscription)
                                        break
                                    case TraceOrderType.FAILED_TRACES:
                                        publishFailedTraces(newSubscription)
                                        break
                                }
                            }
                        } else {
                            log.error("Failed to add artifact subscription", it.cause())
                            request.fail(500, it.cause().message)
                        }
                    })
                }
            })
        })
        vertx.eventBus().consumer(UNSUBSCRIBE_FROM_ARTIFACT_TRACES, { request ->
            def unsubRequest = request.body() as ArtifactTraceUnsubscribeRequest
            core.storage.getSubscriberArtifactSubscriptions(unsubRequest.subscriberUuid(),
                    unsubRequest.appUuid(), unsubRequest.artifactQualifiedName(), {
                if (it.succeeded()) {
                    if (!it.result().isEmpty()) {
                        if (unsubRequest.removeAllArtifactTraceSubscriptions()) {
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
                            def traceSubscriptions = it.result()
                                    .findAll { it.type == TRACES } as List<ArtifactTraceSubscribeRequest>
                            def updatedTraceSubscriptions = new HashMap<ArtifactTraceSubscribeRequest, ArtifactTraceSubscribeRequest>()
                            traceSubscriptions.each {
                                if (unsubRequest.removeTimeFrames().contains(it.timeFrame())) {
                                    updatedTraceSubscriptions.put(it, null)
                                } else {
                                    def updatedOrderTypes = it.orderTypes() - unsubRequest.removeOrderTypes()
                                    if (updatedOrderTypes.isEmpty()) {
                                        updatedTraceSubscriptions.put(it, null)
                                    } else {
                                        updatedTraceSubscriptions.put(it, it.withOrderTypes(updatedOrderTypes))
                                    }
                                }
                            }

                            if (updatedTraceSubscriptions.isEmpty()) {
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
                                updatedTraceSubscriptions.each {
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
                                        log.error("Failed to unsubscribe from artifact traces", it.cause())
                                        request.fail(500, it.cause().message)
                                    }
                                })
                            }
                        }
                    } else {
                        request.reply(true)
                    }
                } else {
                    log.error("Failed to unsubscribe from artifact traces", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void publishLatestTraces(ArtifactTraceSubscribeRequest request) {
        def traceQuery = TraceQuery.builder()
                .orderType(TraceOrderType.LATEST_TRACES)
                .systemRequest(true)
                .appUuid(request.appUuid())
                .artifactQualifiedName(request.artifactQualifiedName())
                .durationStart(Instant.now().minus(request.timeFrame().minutes, ChronoUnit.MINUTES))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        core.traceAPI.getTraces(request.appUuid(), traceQuery, {
            if (it.succeeded()) {
                def traceQueryResult = it.result()
                def traceResult = ArtifactTraceResult.builder()
                        .appUuid(request.appUuid())
                        .artifactQualifiedName(request.artifactQualifiedName())
                        .orderType(traceQuery.orderType())
                        .start(traceQuery.durationStart())
                        .stop(traceQuery.durationStop())
                        .step(traceQuery.durationStep())
                        .traces(traceQueryResult.traces())
                        .total(traceQueryResult.total())
                        .build()
                vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, traceResult)
                log.debug("Published latest traces for artifact: " + traceResult.artifactQualifiedName())
            } else {
                log.error("Failed to get traces", it.cause())
            }
        })
    }

    private void publishSlowestTraces(ArtifactTraceSubscribeRequest request) {
        def traceQuery = TraceQuery.builder()
                .orderType(TraceOrderType.SLOWEST_TRACES)
                .systemRequest(true)
                .appUuid(request.appUuid())
                .artifactQualifiedName(request.artifactQualifiedName())
                .durationStart(Instant.now().minus(request.timeFrame().minutes, ChronoUnit.MINUTES))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        core.traceAPI.getTraces(request.appUuid(), traceQuery, {
            if (it.succeeded()) {
                def traceQueryResult = it.result()
                if (traceQueryResult.traces().isEmpty()) {
                    log.debug("No traces to publish for artifact: " + request.artifactQualifiedName())
                } else {
                    def traceResult = ArtifactTraceResult.builder()
                            .appUuid(request.appUuid())
                            .artifactQualifiedName(request.artifactQualifiedName())
                            .orderType(traceQuery.orderType())
                            .start(traceQuery.durationStart())
                            .stop(traceQuery.durationStop())
                            .step(traceQuery.durationStep())
                            .traces(traceQueryResult.traces())
                            .total(traceQueryResult.total())
                            .build()
                    vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, traceResult)
                    log.debug("Published slowest traces for artifact: " + traceResult.artifactQualifiedName())
                }
            } else {
                log.error("Failed to get traces", it.cause())
            }
        })
    }

    private void publishFailedTraces(ArtifactTraceSubscribeRequest request) {
        def traceQuery = TraceQuery.builder()
                .orderType(TraceOrderType.FAILED_TRACES)
                .systemRequest(true)
                .appUuid(request.appUuid())
                .artifactQualifiedName(request.artifactQualifiedName())
                .durationStart(Instant.now().minus(request.timeFrame().minutes, ChronoUnit.MINUTES))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        core.traceAPI.getTraces(request.appUuid(), traceQuery, {
            if (it.succeeded()) {
                def traceQueryResult = it.result()
                if (traceQueryResult.traces().isEmpty()) {
                    log.debug("No traces to publish for artifact: " + request.artifactQualifiedName())
                } else {
                    def traceResult = ArtifactTraceResult.builder()
                            .appUuid(request.appUuid())
                            .artifactQualifiedName(request.artifactQualifiedName())
                            .orderType(traceQuery.orderType())
                            .start(traceQuery.durationStart())
                            .stop(traceQuery.durationStop())
                            .step(traceQuery.durationStep())
                            .traces(traceQueryResult.traces())
                            .total(traceQueryResult.total())
                            .build()
                    vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, traceResult)
                    log.debug("Published failed traces for artifact: " + traceResult.artifactQualifiedName())
                }
            } else {
                log.error("Failed to get traces", it.cause())
            }
        })
    }
}
