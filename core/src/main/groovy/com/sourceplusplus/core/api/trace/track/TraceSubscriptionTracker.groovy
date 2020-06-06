package com.sourceplusplus.core.api.trace.track

import com.google.common.collect.Sets
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.internal.ApplicationArtifact
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps track of artifact trace subscriptions.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class TraceSubscriptionTracker extends ArtifactSubscriptionTracker {

    public static final String SUBSCRIBE_TO_ARTIFACT_TRACES = "SubscribeToArtifactTraces"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT_TRACES = "UnsubscribeFromArtifactTraces"

    private final Map<ApplicationArtifact, Set<TraceOrderType>> traceSubscriptions

    TraceSubscriptionTracker(SourceCore core) {
        super(core)
        traceSubscriptions = new ConcurrentHashMap<>()
    }

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(UPDATE_ARTIFACT_SUBSCRIPTIONS, {
            traceSubscriptions.each {
                def applicationArtifact = it.key
                def artifactTraceTimeFrames = it.value

                artifactTraceTimeFrames.each {
                    switch (it) {
                        case TraceOrderType.LATEST_TRACES:
                            publishLatestTraces(applicationArtifact)
                            break
                        case TraceOrderType.SLOWEST_TRACES:
                            publishSlowestTraces(applicationArtifact)
                            break
                        case TraceOrderType.FAILED_TRACES:
                            publishFailedTraces(applicationArtifact)
                            break
                    }
                }
            }
        })

        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT_TRACES, {
            def request = it.body() as ArtifactTraceSubscribeRequest
            def appArtifact = ApplicationArtifact.builder()
                    .appUuid(request.appUuid())
                    .artifactQualifiedName(request.artifactQualifiedName()).build()
            traceSubscriptions.putIfAbsent(appArtifact, Sets.newConcurrentHashSet())
            request.orderTypes().each {
                traceSubscriptions.get(appArtifact).add(it)
            }

            request.orderTypes().each {
                switch (it) {
                    case TraceOrderType.LATEST_TRACES:
                        publishLatestTraces(appArtifact)
                        break
                    case TraceOrderType.SLOWEST_TRACES:
                        publishSlowestTraces(appArtifact)
                        break
                    case TraceOrderType.FAILED_TRACES:
                        publishFailedTraces(appArtifact)
                        break
                }
            }
            it.reply(true)
        })
        vertx.eventBus().consumer(UNSUBSCRIBE_FROM_ARTIFACT_TRACES, {
            def request = it.body() as ArtifactTraceUnsubscribeRequest
            def appArtifact = ApplicationArtifact.builder()
                    .appUuid(request.appUuid())
                    .artifactQualifiedName(request.artifactQualifiedName()).build()

            boolean removedArtifactSubscription = false
            if (request.removeAllArtifactTraceSubscriptions()) {
                traceSubscriptions.remove(appArtifact)
                removedArtifactSubscription = true
            } else {
                def appArtifactTimeFrames = traceSubscriptions.get(appArtifact)
                if (appArtifactTimeFrames != null) {
                    request.removeOrderTypes().each {
                        appArtifactTimeFrames.remove(it)
                    }
                    if (appArtifactTimeFrames.isEmpty()) {
                        traceSubscriptions.remove(appArtifact)
                        removedArtifactSubscription = true
                    }
                }
            }
            it.reply(removedArtifactSubscription)
        })
        log.info("{} started", getClass().getSimpleName())
    }

    /**
     * Finds the latest 10 traces for the given ApplicationArtifact and publishes to subscribers.
     * Will look back as far as 30 days.
     *
     * @param appArtifact
     * @param timeFrame
     */
    private void publishLatestTraces(ApplicationArtifact appArtifact) {
        def traceQuery = TraceQuery.builder().orderType(TraceOrderType.LATEST_TRACES)
                .appUuid(appArtifact.appUuid())
                .artifactQualifiedName(appArtifact.artifactQualifiedName())
                .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        core.traceAPI.getTraces(appArtifact.appUuid(), traceQuery, {
            if (it.succeeded()) {
                def traceQueryResult = it.result()
                if (traceQueryResult.traces().isEmpty()) {
                    log.debug("No traces to publish for artifact: " + appArtifact.artifactQualifiedName())
                } else {
                    def traceResult = ArtifactTraceResult.builder()
                            .appUuid(appArtifact.appUuid())
                            .artifactQualifiedName(appArtifact.artifactQualifiedName())
                            .orderType(traceQuery.orderType())
                            .start(traceQuery.durationStart())
                            .stop(traceQuery.durationStop())
                            .step(traceQuery.durationStep())
                            .traces(traceQueryResult.traces())
                            .total(traceQueryResult.total())
                            .build()
                    vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address,
                            new JsonObject(Json.encode(traceResult)))
                    log.debug("Published latest traces for artifact: " + traceResult.artifactQualifiedName())
                }
            } else {
                it.cause().printStackTrace()
            }
        })
    }

    /**
     * Finds the slowest 10 traces for the given ApplicationArtifact and publishes to subscribers.
     * Will look back as far as 30 days.
     *
     * @param appArtifact
     * @param timeFrame
     */
    private void publishSlowestTraces(ApplicationArtifact appArtifact) {
        def traceQuery = TraceQuery.builder().orderType(TraceOrderType.SLOWEST_TRACES)
                .appUuid(appArtifact.appUuid())
                .artifactQualifiedName(appArtifact.artifactQualifiedName())
                .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        core.traceAPI.getTraces(appArtifact.appUuid(), traceQuery, {
            if (it.succeeded()) {
                def traceQueryResult = it.result()
                if (traceQueryResult.traces().isEmpty()) {
                    log.debug("No traces to publish for artifact: " + appArtifact.artifactQualifiedName())
                } else {
                    def traceResult = ArtifactTraceResult.builder()
                            .appUuid(appArtifact.appUuid())
                            .artifactQualifiedName(appArtifact.artifactQualifiedName())
                            .orderType(traceQuery.orderType())
                            .start(traceQuery.durationStart())
                            .stop(traceQuery.durationStop())
                            .step(traceQuery.durationStep())
                            .traces(traceQueryResult.traces())
                            .total(traceQueryResult.total())
                            .build()
                    vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address,
                            new JsonObject(Json.encode(traceResult)))
                    log.debug("Published slowest traces for artifact: " + traceResult.artifactQualifiedName())
                }
            } else {
                it.cause().printStackTrace()
            }
        })
    }

    /**
     * Finds the last failed 10 traces for the given ApplicationArtifact and publishes to subscribers.
     * Will look back as far as 30 days.
     *
     * @param appArtifact
     * @param timeFrame
     */
    private void publishFailedTraces(ApplicationArtifact appArtifact) {
        def traceQuery = TraceQuery.builder().orderType(TraceOrderType.FAILED_TRACES)
                .appUuid(appArtifact.appUuid())
                .artifactQualifiedName(appArtifact.artifactQualifiedName())
                .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        core.traceAPI.getTraces(appArtifact.appUuid(), traceQuery, {
            if (it.succeeded()) {
                def traceQueryResult = it.result()
                if (traceQueryResult.traces().isEmpty()) {
                    log.debug("No traces to publish for artifact: " + appArtifact.artifactQualifiedName())
                } else {
                    def traceResult = ArtifactTraceResult.builder()
                            .appUuid(appArtifact.appUuid())
                            .artifactQualifiedName(appArtifact.artifactQualifiedName())
                            .orderType(traceQuery.orderType())
                            .start(traceQuery.durationStart())
                            .stop(traceQuery.durationStop())
                            .step(traceQuery.durationStep())
                            .traces(traceQueryResult.traces())
                            .total(traceQueryResult.total())
                            .build()
                    vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address,
                            new JsonObject(Json.encode(traceResult)))
                    log.debug("Published failed traces for artifact: " + traceResult.artifactQualifiedName())
                }
            } else {
                it.cause().printStackTrace()
            }
        })
    }
}
