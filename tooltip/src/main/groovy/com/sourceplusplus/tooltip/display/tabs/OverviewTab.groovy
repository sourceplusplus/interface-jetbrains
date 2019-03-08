package com.sourceplusplus.tooltip.display.tabs

import com.codahale.metrics.Histogram
import com.codahale.metrics.UniformReservoir
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.config.SourceTooltipConfig
import com.sourceplusplus.api.model.internal.BarTrendCard
import com.sourceplusplus.api.model.internal.FormattedQuickStats
import com.sourceplusplus.api.model.internal.SplineChart
import com.sourceplusplus.api.model.internal.SplineSeriesData
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetrics
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.tooltip.coordinate.track.TooltipViewTracker
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.DecimalFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Displays general source code artifact statistics.
 * Useful for gathering an overall view of an artifact's runtime behavior.
 *
 * Viewable artifact metrics:
 *  - Average throughput
 *  - Average response time
 *  - 99/95/90/75/50 response time percentiles
 *  - Minimum/Maximum response time
 *  - Average SLA
 *
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class OverviewTab extends AbstractVerticle {

    public static final String OVERVIEW_TAB_OPENED = "OverviewTabOpened"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static DecimalFormat decimalFormat = new DecimalFormat(".#")
    private final static List<MetricType> CARD_METRIC_TYPES =
            [MetricType.Throughput_Average, MetricType.ResponseTime_Average, MetricType.ServiceLevelAgreement_Average]
    private final static List<MetricType> SPLINE_CHART_METRIC_TYPES =
            [MetricType.ResponseTime_99Percentile, MetricType.ResponseTime_95Percentile,
             MetricType.ResponseTime_90Percentile, MetricType.ResponseTime_75Percentile,
             MetricType.ResponseTime_50Percentile]
    private final SourceCoreClient coreClient
    private final boolean pluginAvailable
    private final Map<String, ArtifactMetricResult> metricResultCache = new ConcurrentHashMap<>()

    OverviewTab(SourceCoreClient coreClient, boolean pluginAvailable) {
        this.coreClient = Objects.requireNonNull(coreClient)
        this.pluginAvailable = pluginAvailable
    }

    @Override
    void start() throws Exception {
        //refresh with stats from cache (if avail)
        vertx.eventBus().consumer(OVERVIEW_TAB_OPENED, {
            log.info("Overview tab opened")
            if (pluginAvailable) {
                if (TooltipViewTracker.viewingTooltipArtifact) {
                    def artifactMetricResult = metricResultCache.get(TooltipViewTracker.viewingTooltipArtifact
                            + TooltipViewTracker.currentMetricTimeFrame)
                    if (artifactMetricResult != null) {
                        log.info("Updating overview stats from cache for artifact: " + TooltipViewTracker.viewingTooltipArtifact)
                        updateStats(artifactMetricResult)
                    }
                }
            } else {
                def subscriptions = config().getJsonArray("artifact_subscriptions")
                for (int i = 0; i < subscriptions.size(); i++) {
                    def sub = subscriptions.getJsonObject(i)
                    def artifactQualifiedName = sub.getString("artifact_qualified_name")

                    QueryTimeFrame.values().each {
                        def artifactMetricResult = metricResultCache.get(artifactQualifiedName + it)
                        if (artifactMetricResult != null) {
                            log.info("Updating overview stats from cache for artifact: " + artifactQualifiedName)
                            updateStats(artifactMetricResult)
                        }
                    }
                }
            }
        })
        vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
            def artifactMetricResult = it.body() as ArtifactMetricResult
            metricResultCache.put(artifactMetricResult.artifactQualifiedName()
                    + artifactMetricResult.timeFrame(), artifactMetricResult)

            if (pluginAvailable) {
                if (artifactMetricResult.timeFrame() != TooltipViewTracker.currentMetricTimeFrame) {
                    return //todo: unsub?
                } else if (artifactMetricResult.artifactQualifiedName() != TooltipViewTracker.viewingTooltipArtifact) {
                    return
                }
            }
            updateStats(artifactMetricResult)
        })

        //refresh with stats from cache (if avail) on tooltip opened
        vertx.eventBus().consumer(TooltipViewTracker.OPENED_TOOLTIP, {
            def artifactMetricResult = metricResultCache.get(TooltipViewTracker.viewingTooltipArtifact
                    + TooltipViewTracker.currentMetricTimeFrame)
            if (artifactMetricResult != null) {
                log.info("Updating overview stats from cache for artifact: " + TooltipViewTracker.viewingTooltipArtifact)
                updateStats(artifactMetricResult)
            }
        })

        vertx.eventBus().consumer(TooltipViewTracker.UPDATED_METRIC_TIME_FRAME, {
            if (pluginAvailable) {
                if (TooltipViewTracker.viewingTooltipArtifact == null) {
                    return
                }

                //refresh with stats from cache (if avail)
                def artifactMetricResult = metricResultCache.get(TooltipViewTracker.viewingTooltipArtifact
                        + TooltipViewTracker.currentMetricTimeFrame)
                if (artifactMetricResult != null) {
                    log.info("Updating overview stats from cache for artifact: " + TooltipViewTracker.viewingTooltipArtifact)
                    updateStats(artifactMetricResult)
                }

                //subscribe (re-subscribe) to get latest stats
                def timeFrame = QueryTimeFrame.valueOf(it.body() as String)
                def request = ArtifactMetricSubscribeRequest.builder()
                        .appUuid(SourceTooltipConfig.current.appUuid)
                        .artifactQualifiedName(TooltipViewTracker.viewingTooltipArtifact)
                        .timeFrame(timeFrame)
                        .metricTypes(CARD_METRIC_TYPES + SPLINE_CHART_METRIC_TYPES).build()
                coreClient.subscribeToArtifact(request, {
                    if (it.succeeded()) {
                        log.info("Successfully subscribed to metrics with request: " + request)
                    } else {
                        log.error("Failed to subscribe to artifact metrics", it.cause())
                    }
                })
            } else {
                metricResultCache.values().each { updateStats(it) }

                //subscribe (re-subscribe) to get latest stats
                def subscriptions = config().getJsonArray("artifact_subscriptions")
                for (int i = 0; i < subscriptions.size(); i++) {
                    def sub = subscriptions.getJsonObject(i)
                    QueryTimeFrame.values().each {
                        def request = ArtifactMetricSubscribeRequest.builder()
                                .appUuid(sub.getString("app_uuid"))
                                .artifactQualifiedName(sub.getString("artifact_qualified_name"))
                                .timeFrame(it)
                                .metricTypes(CARD_METRIC_TYPES + SPLINE_CHART_METRIC_TYPES).build()
                        coreClient.subscribeToArtifact(request, {
                            if (it.succeeded()) {
                                log.info("Successfully subscribed to metrics with request: " + request)
                            } else {
                                log.error("Failed to subscribe to artifact metrics", it.cause())
                            }
                        })
                    }
                }
            }
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void updateStats(ArtifactMetricResult artifactMetricResult) {
        log.debug(String.format("Artifact metrics updated. App uuid: %s - Artifact qualified name: %s - Time frame: %s",
                artifactMetricResult.appUuid(), artifactMetricResult.artifactQualifiedName(), artifactMetricResult.timeFrame()))
        artifactMetricResult.artifactMetrics().each {
            updateQuickStats(artifactMetricResult, it)
            if (it.metricType() in CARD_METRIC_TYPES) {
                updateCard(artifactMetricResult, it)
            } else if (it.metricType() in SPLINE_CHART_METRIC_TYPES) {
                updateSplineGraph(artifactMetricResult, it)
            } else {
                throw new UnsupportedOperationException("Invalid metric type: " + it)
            }
        }
    }

    private void updateSplineGraph(ArtifactMetricResult metricResult, ArtifactMetrics artifactMetrics) {
        def appUuid = metricResult.appUuid()
        def artifactQualifiedName = metricResult.artifactQualifiedName()
        def times = new ArrayList<Instant>()
        def current = metricResult.start()
        times.add(current)
        while (current.isBefore(metricResult.stop())) {
            if (metricResult.step() == "MINUTE") {
                current = current.plus(1, ChronoUnit.MINUTES)
                times.add(current)
            } else {
                throw new UnsupportedOperationException("Invalid step: " + metricResult.step())
            }
        }

        def seriesDataBuilder = SplineSeriesData.builder()
                .times(times)
                .values(artifactMetrics.values() as double[])
        switch (artifactMetrics.metricType()) {
            case MetricType.ResponseTime_99Percentile:
                seriesDataBuilder.seriesIndex(0)
                break
            case MetricType.ResponseTime_95Percentile:
                seriesDataBuilder.seriesIndex(1)
                break
            case MetricType.ResponseTime_90Percentile:
                seriesDataBuilder.seriesIndex(2)
                break
            case MetricType.ResponseTime_75Percentile:
                seriesDataBuilder.seriesIndex(3)
                break
            case MetricType.ResponseTime_50Percentile:
                seriesDataBuilder.seriesIndex(4)
                break
            default:
                throw new UnsupportedOperationException("Invalid metric type: " + artifactMetrics.metricType())
        }
        def splintChart = SplineChart.builder()
                .timeFrame(metricResult.timeFrame())
                .addSeriesData(seriesDataBuilder.build())
                .build()
        if (pluginAvailable) {
            vertx.eventBus().publish("UpdateChart", new JsonObject(Json.encode(splintChart)))
        } else {
            vertx.eventBus().publish("$appUuid-$artifactQualifiedName-UpdateChart",
                    new JsonObject(Json.encode(splintChart)))
        }
    }

    private void updateQuickStats(ArtifactMetricResult metricResult, ArtifactMetrics artifactMetrics) {
        def formattedStats = FormattedQuickStats.builder()
                .timeFrame(metricResult.timeFrame())
        def avg = artifactMetrics.values().sum() / artifactMetrics.values().size()
        switch (artifactMetrics.metricType()) {
            case MetricType.ResponseTime_50Percentile:
                formattedStats.p50(toPrettyDuration(avg as int))
                        .min(toPrettyDuration(artifactMetrics.values().min()))
                break
            case MetricType.ResponseTime_75Percentile:
                formattedStats.p75(toPrettyDuration(avg as int))
                break
            case MetricType.ResponseTime_90Percentile:
                formattedStats.p90(toPrettyDuration(avg as int))
                break
            case MetricType.ResponseTime_95Percentile:
                formattedStats.p95(toPrettyDuration(avg as int))
                break
            case MetricType.ResponseTime_99Percentile:
                formattedStats.p99(toPrettyDuration(avg as int))
                        .max(toPrettyDuration(artifactMetrics.values().max()))
                break
            default:
                return
        }

        if (pluginAvailable) {
            vertx.eventBus().publish("DisplayStats", new JsonObject(Json.encode(formattedStats.build())))
        } else {
            def appUuid = metricResult.appUuid()
            def artifactQualifiedName = metricResult.artifactQualifiedName()
            vertx.eventBus().publish("$appUuid-$artifactQualifiedName-DisplayStats",
                    new JsonObject(Json.encode(formattedStats.build())))
        }
    }

    private void updateCard(ArtifactMetricResult metricResult, ArtifactMetrics artifactMetrics) {
        def appUuid = metricResult.appUuid()
        def artifactQualifiedName = metricResult.artifactQualifiedName()
        def histogram = new Histogram(new UniformReservoir(artifactMetrics.values().size()))
        def metricArr = new ArrayList<Integer>()
        if (artifactMetrics.values().size() == 60) {
            for (int i = 0; i < artifactMetrics.values().size(); i += 4) {
                metricArr.add(artifactMetrics.values().get(i) + artifactMetrics.values().get(i + 1)
                        + artifactMetrics.values().get(i + 2) + artifactMetrics.values().get(i + 3))
            }
        } else if (artifactMetrics.values().size() == 30) {
            for (int i = 0; i < artifactMetrics.values().size(); i += 2) {
                metricArr.add(artifactMetrics.values().get(i) + artifactMetrics.values().get(i + 1))
            }
        } else {
            metricArr.addAll(artifactMetrics.values())
        }

        def percentMax = metricArr.max()
        def percents = new ArrayList<Double>()
        artifactMetrics.values().each {
            histogram.update(it)
        }
        for (int i = 0; i < metricArr.size(); i++) {
            if (percentMax == 0) {
                percents.add(0)
            } else {
                percents.add((metricArr.get(i) / percentMax) * 100.00)
            }
        }
        def avg = histogram.snapshot.mean

        if (artifactMetrics.metricType() == MetricType.Throughput_Average) {
            def barTrendCard = BarTrendCard.builder()
                    .timeFrame(metricResult.timeFrame())
                    .header(toPrettyFrequency(avg / 60.0))
                    .meta(artifactMetrics.metricType().toString().toLowerCase())
                    .barGraphData(percents as double[])
                    .build()
            if (pluginAvailable) {
                vertx.eventBus().publish("DisplayCard", new JsonObject(Json.encode(barTrendCard)))
            } else {
                vertx.eventBus().publish("$appUuid-$artifactQualifiedName-DisplayCard",
                        new JsonObject(Json.encode(barTrendCard)))
            }
        } else if (artifactMetrics.metricType() == MetricType.ResponseTime_Average) {
            def barTrendCard = BarTrendCard.builder()
                    .timeFrame(metricResult.timeFrame())
                    .header(toPrettyDuration(avg as int))
                    .meta(artifactMetrics.metricType().toString().toLowerCase())
                    .barGraphData(percents as double[])
                    .build()
            if (pluginAvailable) {
                vertx.eventBus().publish("DisplayCard", new JsonObject(Json.encode(barTrendCard)))
            } else {
                vertx.eventBus().publish("$appUuid-$artifactQualifiedName-DisplayCard",
                        new JsonObject(Json.encode(barTrendCard)))
            }
        } else if (artifactMetrics.metricType() == MetricType.ServiceLevelAgreement_Average) {
            def barTrendCard = BarTrendCard.builder()
                    .timeFrame(metricResult.timeFrame())
                    .header(decimalFormat.format(avg / 100.0))
                    .meta(artifactMetrics.metricType().toString().toLowerCase())
                    .barGraphData(percents as double[])
                    .build()
            if (pluginAvailable) {
                vertx.eventBus().publish("DisplayCard", new JsonObject(Json.encode(barTrendCard)))
            } else {
                vertx.eventBus().publish("$appUuid-$artifactQualifiedName-DisplayCard",
                        new JsonObject(Json.encode(barTrendCard)))
            }
        } else {
            throw new UnsupportedOperationException("Invalid metric type: " + artifactMetrics.metricType())
        }
    }

    private static String toPrettyDuration(int millis) {
        def days = millis / 86400000d
        if (days > 1) {
            return (days as int) + "dys"
        }
        def hours = millis / 3600000d
        if (hours > 1) {
            return (hours as int) + "hrs"
        }
        def minutes = millis / 60000d
        if (minutes > 1) {
            return (minutes as int) + "mins"
        }
        def seconds = millis / 1000d
        if (seconds > 1) {
            return (seconds as int) + "secs"
        }
        return (millis as int) + "ms"
    }

    private static String toPrettyFrequency(double perSecond) {
        if (perSecond > 1000000) {
            return (perSecond / 1000000 as int) + "M/sec"
        } else if (perSecond > 1000) {
            return (perSecond / 1000 as int) + "K/sec"
        } else {
            return (perSecond * 60 as int) + "/min"
        }
    }
}
