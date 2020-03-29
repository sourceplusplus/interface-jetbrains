package com.sourceplusplus.portal.display.tabs

import com.codahale.metrics.Histogram
import com.codahale.metrics.UniformReservoir
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.api.model.internal.BarTrendCard
import com.sourceplusplus.api.model.internal.SplineChart
import com.sourceplusplus.api.model.internal.SplineSeriesData
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetrics
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.display.PortalTab
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

import java.text.DecimalFormat
import java.time.Instant
import java.time.temporal.ChronoUnit

import static com.sourceplusplus.api.model.metric.MetricType.*

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
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class OverviewTab extends AbstractTab {

    public static final String OVERVIEW_TAB_OPENED = "OverviewTabOpened"
    public static final String SET_METRIC_TIME_FRAME = "SetMetricTimeFrame"
    public static final String SET_ACTIVE_CHART_METRIC = "SetActiveChartMetric"

    private static DecimalFormat decimalFormat = new DecimalFormat(".#")

    OverviewTab() {
        super(PortalTab.Overview)
    }

    @Override
    void start() throws Exception {
        super.start()

        //refresh with stats from cache (if avail)
        vertx.eventBus().consumer(OVERVIEW_TAB_OPENED, {
            log.info("Overview tab opened")
            def portalUuid = (it.body() as JsonObject).getString("portal_uuid")
            def portal = SourcePortal.getPortal(portalUuid)
            portal.interface.currentTab = PortalTab.Overview
            updateUI(portal)
            SourcePortal.ensurePortalActive(portal)
        })
        vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
            def artifactMetricResult = it.body() as ArtifactMetricResult
            SourcePortal.getPortals(artifactMetricResult.appUuid(), artifactMetricResult.artifactQualifiedName()).each {
                it.interface.overviewView.cacheMetricResult(artifactMetricResult)
                updateUI(it)
            }
        })

        vertx.eventBus().consumer(SET_METRIC_TIME_FRAME, {
            def request = JsonObject.mapFrom(it.body())
            def portal = SourcePortal.getPortal(request.getString("portal_uuid"))
            def view = portal.interface.overviewView
            view.timeFrame = QueryTimeFrame.valueOf(request.getString("metric_time_frame").toUpperCase())
            log.info("Overview time frame set to: " + view.timeFrame)
            updateUI(portal)

            //subscribe (re-subscribe) to get latest stats
            def subscribeRequest = ArtifactMetricSubscribeRequest.builder()
                    .appUuid(portal.appUuid)
                    .artifactQualifiedName(portal.interface.viewingPortalArtifact)
                    .timeFrame(view.timeFrame)
                    .metricTypes([Throughput_Average, ResponseTime_Average, ServiceLevelAgreement_Average]).build()
            SourcePortalConfig.current.getCoreClient(portal.appUuid).subscribeToArtifact(subscribeRequest, {
                if (it.succeeded()) {
                    log.info("Successfully subscribed to metrics with request: " + subscribeRequest)
                } else {
                    log.error("Failed to subscribe to artifact metrics", it.cause())
                }
            })
        })
        vertx.eventBus().consumer(SET_ACTIVE_CHART_METRIC, {
            def request = JsonObject.mapFrom(it.body())
            def portal = SourcePortal.getPortal(request.getString("portal_uuid"))
            portal.interface.overviewView.activeChartMetric = valueOf(request.getString("metric_type"))
            updateUI(portal)
        })
        log.info("{} started", getClass().getSimpleName())
    }

    @Override
    void updateUI(SourcePortal portal) {
        if (portal.interface.currentTab != thisTab) {
            return
        }

        def artifactMetricResult = portal.interface.overviewView.metricResult
        if (artifactMetricResult) {
            if (log.traceEnabled) {
                log.trace(String.format("Artifact metrics updated. Portal uuid: %s - App uuid: %s - Artifact qualified name: %s - Time frame: %s",
                        portal.portalUuid, artifactMetricResult.appUuid(), artifactMetricResult.artifactQualifiedName(), artifactMetricResult.timeFrame()))
            }

            artifactMetricResult.artifactMetrics().each {
                updateCard(portal, artifactMetricResult, it)
                if (it.metricType() == portal.interface.overviewView.activeChartMetric) {
                    updateSplineGraph(portal, artifactMetricResult, it)
                }
            }
        }
    }

    private void updateSplineGraph(SourcePortal portal, ArtifactMetricResult metricResult,
                                   ArtifactMetrics artifactMetrics) {
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
        if (artifactMetrics.metricType() == ServiceLevelAgreement_Average) {
            artifactMetrics = artifactMetrics.withValues(artifactMetrics.values().collect { it / 100 } as int[])
        }

        def seriesDataBuilder = SplineSeriesData.builder()
                .times(times)
                .values(artifactMetrics.values() as double[])
        switch (artifactMetrics.metricType()) {
            case ResponseTime_99Percentile:
                seriesDataBuilder.seriesIndex(0)
                break
            case ResponseTime_95Percentile:
                seriesDataBuilder.seriesIndex(1)
                break
            case ResponseTime_90Percentile:
                seriesDataBuilder.seriesIndex(2)
                break
            case ResponseTime_75Percentile:
                seriesDataBuilder.seriesIndex(3)
                break
            case ResponseTime_50Percentile:
                seriesDataBuilder.seriesIndex(4)
                break
            default:
                seriesDataBuilder.seriesIndex(0)
        }
        def splintChart = SplineChart.builder()
                .metricType(artifactMetrics.metricType())
                .timeFrame(metricResult.timeFrame())
                .addSeriesData(seriesDataBuilder.build())
                .build()

        def portalUuid = portal.portalUuid
        vertx.eventBus().publish("$portalUuid-UpdateChart", new JsonObject(Json.encode(splintChart)))
    }

    private void updateCard(SourcePortal portal, ArtifactMetricResult metricResult, ArtifactMetrics artifactMetrics) {
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

        if (artifactMetrics.metricType() == Throughput_Average) {
            def barTrendCard = BarTrendCard.builder()
                    .timeFrame(metricResult.timeFrame())
                    .header(toPrettyFrequency(avg / 60.0))
                    .meta(artifactMetrics.metricType().toString().toLowerCase())
                    .barGraphData(percents as double[])
                    .build()
            def portalUuid = portal.portalUuid
            vertx.eventBus().publish("$portalUuid-DisplayCard", new JsonObject(Json.encode(barTrendCard)))
        } else if (artifactMetrics.metricType() == ResponseTime_Average) {
            def barTrendCard = BarTrendCard.builder()
                    .timeFrame(metricResult.timeFrame())
                    .header(toPrettyDuration(avg as int))
                    .meta(artifactMetrics.metricType().toString().toLowerCase())
                    .barGraphData(percents as double[])
                    .build()
            def portalUuid = portal.portalUuid
            vertx.eventBus().publish("$portalUuid-DisplayCard", new JsonObject(Json.encode(barTrendCard)))
        } else if (artifactMetrics.metricType() == ServiceLevelAgreement_Average) {
            def barTrendCard = BarTrendCard.builder()
                    .timeFrame(metricResult.timeFrame())
                    .header(decimalFormat.format(avg / 100.0))
                    .meta(artifactMetrics.metricType().toString().toLowerCase())
                    .barGraphData(percents as double[])
                    .build()
            def portalUuid = portal.portalUuid
            vertx.eventBus().publish("$portalUuid-DisplayCard", new JsonObject(Json.encode(barTrendCard)))
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
