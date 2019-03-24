package com.sourceplusplus.core.api.trace.track

import com.google.common.collect.Sets
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.internal.ApplicationArtifact
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import com.sourceplusplus.core.api.trace.TraceAPI
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * todo: description
 *
 * @version 0.1.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TraceSubscriptionTracker extends ArtifactSubscriptionTracker {

    public static final String SUBSCRIBE_TO_ARTIFACT_TRACES = "SubscribeToArtifactTraces"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT_TRACES = "UnsubscribeFromArtifactTraces"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final TraceAPI traceAPI
    private final Map<ApplicationArtifact, Set<TraceOrderType>> traceSubscriptions

    TraceSubscriptionTracker(ArtifactAPI artifactAPI, TraceAPI traceAPI) {
        super(artifactAPI)
        this.traceAPI = Objects.requireNonNull(traceAPI)
        traceSubscriptions = new ConcurrentHashMap<>()
    }

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(UPDATE_ARTIFACT_SUBSCRIPTIONS, {
            traceSubscriptions.each {
                def applicationArtifact = it.key
                def artifactTraceTimeFrames = it.value

                artifactTraceTimeFrames.each {
                    publishLatestTraces(applicationArtifact, it)
                }
            }
        })

        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT_TRACES, {
            def request = it.body() as ArtifactTraceSubscribeRequest
            def appArtifact = ApplicationArtifact.builder()
                    .appUuid(request.appUuid())
                    .artifactQualifiedName(request.artifactQualifiedName()).build()
            traceSubscriptions.putIfAbsent(appArtifact, Sets.newConcurrentHashSet())
            traceSubscriptions.get(appArtifact).add(request.orderType())

            publishLatestTraces(appArtifact, request.orderType())
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
    private void publishLatestTraces(ApplicationArtifact appArtifact, TraceOrderType timeFrame) {
        def traceQuery = TraceQuery.builder()
                .appUuid(appArtifact.appUuid())
                .artifactQualifiedName(appArtifact.artifactQualifiedName())
                .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        traceAPI.getTraces(appArtifact.appUuid(), traceQuery, {
            if (it.succeeded()) {
                def traceQueryResult = it.result()
                if (traceQueryResult.traces().isEmpty()) {
                    log.info("No new traces to publish for artifact: " + appArtifact.artifactQualifiedName())
                } else {
                    def traceResult = ArtifactTraceResult.builder()
                            .appUuid(appArtifact.appUuid())
                            .artifactQualifiedName(appArtifact.artifactQualifiedName())
                            .timeFrame(timeFrame)
                            .start(traceQuery.durationStart())
                            .stop(traceQuery.durationStop())
                            .step(traceQuery.durationStep())
                            .traces(traceQueryResult.traces())
                            .total(traceQueryResult.total())
                            .build()
                    vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address,
                            new JsonObject(Json.encode(traceResult)))
                    log.debug("Published updated traces for artifact: " + traceResult.artifactQualifiedName())
                }
            } else {
                it.cause().printStackTrace()
            }
        })
    }
}
