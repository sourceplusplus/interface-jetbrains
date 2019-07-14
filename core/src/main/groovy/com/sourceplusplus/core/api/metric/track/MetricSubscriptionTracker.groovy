package com.sourceplusplus.core.api.metric.track

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.internal.ApplicationArtifact
import com.sourceplusplus.api.model.metric.ArtifactMetricQuery
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
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
 * @version 0.2.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class MetricSubscriptionTracker extends ArtifactSubscriptionTracker {

    public static final String SUBSCRIBE_TO_ARTIFACT_METRICS = "SubscribeToArtifactMetrics"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT_METRICS = "UnsubscribeFromArtifactMetrics"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final Map<ApplicationArtifact, Map<QueryTimeFrame, Set<MetricType>>> metricSubscriptions

    MetricSubscriptionTracker(SourceCore core) {
        super(core)
        metricSubscriptions = new ConcurrentHashMap<>()
    }

    @Override
    void start() throws Exception {
        vertx.eventBus().consumer(UPDATE_ARTIFACT_SUBSCRIPTIONS, {
            metricSubscriptions.each {
                def applicationArtifact = it.key
                def artifactMetricTimeFrames = it.value

                artifactMetricTimeFrames.each {
                    def timeFrame = it.key
                    def metricTypes = it.value as HashSet<MetricType>
                    publishLatestMetrics(applicationArtifact, timeFrame, metricTypes.asList())
                }
            }
        })

        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT_METRICS, {
            def request = it.body() as ArtifactMetricSubscribeRequest
            def appArtifact = ApplicationArtifact.builder()
                    .appUuid(request.appUuid())
                    .artifactQualifiedName(request.artifactQualifiedName()).build()

            metricSubscriptions.putIfAbsent(appArtifact, Maps.newConcurrentMap())
            def appArtifactTimeFrames = metricSubscriptions.get(appArtifact)
            appArtifactTimeFrames.putIfAbsent(request.timeFrame(), Sets.newConcurrentHashSet())
            def metricTypes = appArtifactTimeFrames.get(request.timeFrame())
            metricTypes.addAll(request.metricTypes())

            publishLatestMetrics(appArtifact, request.timeFrame(), request.metricTypes())
            it.reply(true)
        })
        vertx.eventBus().consumer(UNSUBSCRIBE_FROM_ARTIFACT_METRICS, {
            def request = it.body() as ArtifactMetricUnsubscribeRequest
            def appArtifact = ApplicationArtifact.builder()
                    .appUuid(request.appUuid())
                    .artifactQualifiedName(request.artifactQualifiedName()).build()

            boolean removedArtifactSubscription = false
            if (request.removeAllArtifactMetricSubscriptions()) {
                metricSubscriptions.remove(appArtifact)
                removedArtifactSubscription = true
            } else {
                def appArtifactTimeFrames = metricSubscriptions.get(appArtifact)
                if (appArtifactTimeFrames != null) {
                    request.removeTimeFrames().each {
                        appArtifactTimeFrames.remove(it)
                    }
                    request.removeTimeFramedMetricTypes().each {
                        def metricTypes = appArtifactTimeFrames.get(it.timeFrame())
                        metricTypes?.remove(it.metricType())
                    }
                    request.removeMetricTypes().each { removeMetricType ->
                        appArtifactTimeFrames.each {
                            it.value.remove(removeMetricType)
                        }
                    }
                    if (appArtifactTimeFrames.isEmpty()) {
                        metricSubscriptions.remove(appArtifact)
                        removedArtifactSubscription = true
                    }
                }
            }
            it.reply(removedArtifactSubscription)
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void publishLatestMetrics(ApplicationArtifact appArtifact, QueryTimeFrame timeFrame,
                                      List<MetricType> metricTypes) {
        def metricQuery = ArtifactMetricQuery.builder()
                .appUuid(appArtifact.appUuid())
                .artifactQualifiedName(appArtifact.artifactQualifiedName())
                .metricTypes(metricTypes)
                .timeFrame(timeFrame)
                .start(Instant.now().minus(timeFrame.minutes - 1, ChronoUnit.MINUTES))
                .stop(Instant.now())
                .step("MINUTE")
                .build()
        core.metricAPI.getArtifactMetrics(metricQuery, {
            if (it.succeeded()) {
                def metricResult = it.result()
                vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address,
                        new JsonObject(Json.encode(metricResult)))
                log.debug("Published updated metrics for artifact: " + metricResult.artifactQualifiedName())
            } else {
                it.cause().printStackTrace()
            }
        })
    }
}
