package com.sourceplusplus.portal.display

import com.codahale.metrics.Histogram
import com.codahale.metrics.UniformReservoir
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.extensions.displayCard
import com.sourceplusplus.portal.extensions.updateChart
import com.sourceplusplus.protocol.ArtifactNameUtils.getShortQualifiedFunctionName
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ArtifactMetricUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.OverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.RefreshOverview
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.SetActiveChartMetric
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.SetMetricTimeFrame
import com.sourceplusplus.protocol.ProtocolAddress.Portal.Companion.ClearOverview
import com.sourceplusplus.protocol.artifact.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.ArtifactMetrics
import com.sourceplusplus.protocol.portal.*
import com.sourceplusplus.protocol.portal.MetricType.*
import io.vertx.core.json.JsonObject
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList

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
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class OverviewDisplay : AbstractDisplay(PageType.OVERVIEW) {

    companion object {
        private val log = LoggerFactory.getLogger(OverviewDisplay::class.java)
        private val decimalFormat = DecimalFormat(".#")
    }

    override suspend fun start() {
        super.start()

        vertx.setPeriodic(5000) {
            SourcePortal.getPortals().forEach {
                if (it.currentTab == PageType.OVERVIEW) {
                    //todo: only update if external or internal and currently displaying
                    vertx.eventBus().send(RefreshOverview, it)
                }
            }
        }

        //refresh with stats from cache (if avail)
        vertx.eventBus().consumer<JsonObject>(OverviewTabOpened) {
            log.info("Overview tab opened")
            val portalUuid = it.body().getString("portal_uuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            portal.currentTab = thisTab
            SourcePortal.ensurePortalActive(portal)
            updateUI(portal)
        }
        vertx.eventBus().consumer<ArtifactMetricResult>(ArtifactMetricUpdated) {
            val artifactMetricResult = it.body()
            SourcePortal.getPortals(artifactMetricResult.appUuid!!, artifactMetricResult.artifactQualifiedName)
                .forEach { portal ->
                    portal.overviewView.cacheMetricResult(artifactMetricResult)
                    updateUI(portal)
                }
        }

        vertx.eventBus().consumer<JsonObject>(SetMetricTimeFrame) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portal_uuid"))!!
            val view = portal.overviewView
            view.timeFrame = QueryTimeFrame.valueOf(request.getString("metric_time_frame").toUpperCase())
            log.info("Overview time frame set to: " + view.timeFrame)
            updateUI(portal)

            vertx.eventBus().send(RefreshOverview, portal)
        }
        vertx.eventBus().consumer<JsonObject>(SetActiveChartMetric) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portal_uuid"))!!
            portal.overviewView.activeChartMetric = valueOf(request.getString("metric_type"))
            updateUI(portal)

            vertx.eventBus().send(ClearOverview(portal.portalUuid), null)
            vertx.eventBus().send(RefreshOverview, portal)
        }
        log.info("{} started", javaClass.simpleName)
    }

    override fun updateUI(portal: SourcePortal) {
        if (portal.currentTab != thisTab) {
            return
        }

        val artifactMetricResult = portal.overviewView.metricResult ?: return
        if (log.isTraceEnabled) {
            log.trace(
                "Artifact metrics updated. Portal uuid: {} - App uuid: {} - Artifact qualified name: {} - Time frame: {}",
                portal.portalUuid,
                artifactMetricResult.appUuid,
                getShortQualifiedFunctionName(artifactMetricResult.artifactQualifiedName),
                artifactMetricResult.timeFrame
            )
        }

        artifactMetricResult.artifactMetrics.forEach {
            updateCard(portal, artifactMetricResult, it)
            if ((it.metricType.responseTimePercentile
                        && portal.overviewView.activeChartMetric == ResponseTime_Average)
                || it.metricType == portal.overviewView.activeChartMetric
            ) {
                updateSplineGraph(portal, artifactMetricResult, it)
            }
        }
    }

    fun updateSplineGraph(portal: SourcePortal, metricResult: ArtifactMetricResult, artifactMetrics: ArtifactMetrics) {
        val times = ArrayList<Instant>() //todo: no toJavaInstant/fromEpochMilliseconds
        var current = metricResult.start
        times.add(current)
        while (current.toJavaInstant().isBefore(metricResult.stop.toJavaInstant())) {
            if (metricResult.step == "MINUTE") {
                current = Instant.fromEpochMilliseconds(
                    current.toJavaInstant().plus(1, ChronoUnit.MINUTES).toEpochMilli()
                )
                times.add(current)
            } else {
                throw UnsupportedOperationException("Invalid step: " + metricResult.step)
            }
        }

        val finalArtifactMetrics = if (artifactMetrics.metricType == ServiceLevelAgreement_Average) {
            artifactMetrics.copy(values = artifactMetrics.values.map { it / 100 })
        } else {
            artifactMetrics
        }

        val seriesIndex =
            when (finalArtifactMetrics.metricType) {
                ResponseTime_99Percentile -> 0
                ResponseTime_95Percentile -> 1
                ResponseTime_90Percentile -> 2
                ResponseTime_75Percentile -> 3
                ResponseTime_50Percentile -> 4
                else -> 0
            }
        val seriesData = SplineSeriesData(
            seriesIndex = seriesIndex,
            times = times.map { it.epochSeconds }, //todo: no epochSeconds
            values = finalArtifactMetrics.values.map { it.toDouble() }.toDoubleArray() //todo: or this
        )
        val splineChart = SplineChart(
            metricType = finalArtifactMetrics.metricType,
            timeFrame = metricResult.timeFrame,
            seriesData = Collections.singletonList(seriesData)
        )
        vertx.eventBus().updateChart(portal.portalUuid, splineChart)
    }

    fun updateCard(portal: SourcePortal, metricResult: ArtifactMetricResult, artifactMetrics: ArtifactMetrics) {
        val avg = calculateAverage(artifactMetrics)
        val percents = calculatePercents(artifactMetrics)

        when (artifactMetrics.metricType) {
            Throughput_Average -> {
                val barTrendCard = BarTrendCard(
                    timeFrame = metricResult.timeFrame,
                    header = toPrettyFrequency(avg / 60.0),
                    meta = artifactMetrics.metricType.toString().toLowerCase(),
                    barGraphData = percents
                )
                vertx.eventBus().displayCard(portal.portalUuid, barTrendCard)
            }
            ResponseTime_Average -> {
                val barTrendCard = BarTrendCard(
                    timeFrame = metricResult.timeFrame,
                    header = toPrettyDuration(avg.toInt()),
                    meta = artifactMetrics.metricType.toString().toLowerCase(),
                    barGraphData = percents
                )
                vertx.eventBus().displayCard(portal.portalUuid, barTrendCard)
            }
            ServiceLevelAgreement_Average -> {
                val barTrendCard = BarTrendCard(
                    timeFrame = metricResult.timeFrame,
                    header = if (avg == 0.0) {
                        "0%"
                    } else {
                        decimalFormat.format(avg / 100.0).toString() + "%"
                    },
                    meta = artifactMetrics.metricType.toString().toLowerCase(),
                    barGraphData = percents
                )
                vertx.eventBus().displayCard(portal.portalUuid, barTrendCard)
            }
        }
    }

    private fun calculateAverage(artifactMetrics: ArtifactMetrics): Double {
        val histogram = Histogram(UniformReservoir(artifactMetrics.values.size))
        artifactMetrics.values.forEach {
            histogram.update(it)
        }
        return histogram.snapshot.mean
    }

    private fun calculatePercents(artifactMetrics: ArtifactMetrics): DoubleArray {
        val metricArr = ArrayList<Int>()
        when (artifactMetrics.values.size) {
            60 -> {
                for (i in artifactMetrics.values.indices) {
                    metricArr.add(
                        artifactMetrics.values[i] + artifactMetrics.values[i + 1] +
                                artifactMetrics.values[i + 2] + artifactMetrics.values[i + 3]
                    )
                }
            }
            30 -> {
                for (i in artifactMetrics.values.indices step 2) {
                    metricArr.add(artifactMetrics.values[i] + artifactMetrics.values[i + 1])
                }
            }
            else -> {
                metricArr.addAll(artifactMetrics.values)
            }
        }

        val percentMax = metricArr.maxOrNull()!!
        val percents = ArrayList<Double>()
        for (i in metricArr.indices) {
            if (percentMax == 0) {
                percents.add(0.0)
            } else {
                percents.add((metricArr[i] / percentMax) * 100.00)
            }
        }
        return percents.toDoubleArray()
    }

    private fun toPrettyDuration(millis: Int): String {
        val days = millis / 86400000.0
        if (days > 1) {
            return "${days.toInt()}dys"
        }
        val hours = millis / 3600000.0
        if (hours > 1) {
            return "${hours.toInt()}hrs"
        }
        val minutes = millis / 60000.0
        if (minutes > 1) {
            return "${minutes.toInt()}mins"
        }
        val seconds = millis / 1000.0
        if (seconds > 1) {
            return "${seconds.toInt()}secs"
        }
        return "${millis}ms"
    }

    private fun toPrettyFrequency(perSecond: Double): String {
        return when {
            perSecond > 1000000.0 -> "${perSecond / 1000000.0.toInt()}M/sec"
            perSecond > 1000.0 -> "${perSecond / 1000.0.toInt()}K/sec"
            perSecond > 1.0 -> "${perSecond.toInt()}/sec"
            else -> "${(perSecond * 60.0).toInt()}/min"
        }
    }
}
