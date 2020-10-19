package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.extensions.displayCard
import com.sourceplusplus.portal.extensions.fromPerSecondToPrettyFrequency
import com.sourceplusplus.portal.extensions.toPrettyDuration
import com.sourceplusplus.portal.extensions.updateChart
import com.sourceplusplus.portal.model.PageType
import com.sourceplusplus.protocol.ArtifactNameUtils.getShortQualifiedFunctionName
import com.sourceplusplus.protocol.ProtocolAddress.Global.ActivityTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactMetricUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshActivity
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetActiveChartMetric
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetMetricTimeFrame
import com.sourceplusplus.protocol.ProtocolAddress.Portal.ClearActivity
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.metrics.*
import com.sourceplusplus.protocol.artifact.metrics.MetricType.*
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
class ActivityDisplay : AbstractDisplay(PageType.ACTIVITY) {

    companion object {
        private val log = LoggerFactory.getLogger(ActivityDisplay::class.java)
        private val decimalFormat = DecimalFormat(".#")
    }

    override suspend fun start() {
        vertx.setPeriodic(5000) {
            SourcePortal.getPortals().forEach {
                if (it.currentTab == PageType.ACTIVITY) {
                    //todo: only update if external or internal and currently displaying
                    vertx.eventBus().send(RefreshActivity, it)
                }
            }
        }

        //refresh with stats from cache (if avail)
        vertx.eventBus().consumer<JsonObject>(ActivityTabOpened) {
            log.info("Activity tab opened")
            val portalUuid = it.body().getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            portal.currentTab = thisTab
            SourcePortal.ensurePortalActive(portal)
            updateUI(portal)

            //for some reason clearing (resizing) the activity chart is necessary once SourceMarkerPlugin.init()
            //has been called more than once; for now just do it whenever the activity tab is opened
            vertx.eventBus().send(ClearActivity(portal.portalUuid), null)
        }
        vertx.eventBus().consumer<ArtifactMetricResult>(ArtifactMetricUpdated) {
            val artifactMetricResult = it.body()
            SourcePortal.getPortals(artifactMetricResult.appUuid, artifactMetricResult.artifactQualifiedName)
                .forEach { portal ->
                    portal.activityView.cacheMetricResult(artifactMetricResult)
                    updateUI(portal)
                }
        }

        vertx.eventBus().consumer<JsonObject>(SetMetricTimeFrame) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
            val view = portal.activityView
            view.timeFrame = QueryTimeFrame.valueOf(request.getString("metricTimeFrame").toUpperCase())
            log.info("Activity time frame set to: " + view.timeFrame)
            updateUI(portal)

            vertx.eventBus().send(RefreshActivity, portal)
        }
        vertx.eventBus().consumer<JsonObject>(SetActiveChartMetric) {
            val request = JsonObject.mapFrom(it.body())
            val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
            portal.activityView.activeChartMetric = valueOf(request.getString("metricType"))
            updateUI(portal)

            vertx.eventBus().send(ClearActivity(portal.portalUuid), null)
            vertx.eventBus().send(RefreshActivity, portal)
        }
        log.info("{} started", javaClass.simpleName)
    }

    override fun updateUI(portal: SourcePortal) {
        if (portal.currentTab != thisTab) {
            return
        }

        val artifactMetricResult = portal.activityView.metricResult ?: return
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
                        && portal.activityView.activeChartMetric == ResponseTime_Average)
                || it.metricType == portal.activityView.activeChartMetric
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
            times = times.map { Instant.fromEpochMilliseconds(it.toEpochMilliseconds()) },
            values = finalArtifactMetrics.values
        )
        val splineChart = SplineChart(
            metricType = finalArtifactMetrics.metricType,
            timeFrame = metricResult.timeFrame,
            seriesData = Collections.singletonList(seriesData)
        )
        vertx.eventBus().updateChart(portal.portalUuid, splineChart)

//        if (portal.advice.isNotEmpty()) {
//            for (advice in portal.advice) {
//                if (advice is RampDetectionAdvice) {
//                    val regressionSeriesData = SplineSeriesData(
//                        seriesIndex = 5,
//                        times = times.map { it.epochSeconds }, //todo: no epochSeconds
//                        values = times.mapIndexed { i, it ->
//                            if (splineChart.seriesData[0].values[i] == 0.0) {
//                                0.0
//                            } else {
//                                advice.regression.predict(it.toEpochMilliseconds().toDouble())
//                            }
//                        }
//                    )
//                    val regressionSplineChart = splineChart.copy(
//                        seriesData = Collections.singletonList(regressionSeriesData)
//                    )
//                    vertx.eventBus().updateChart(portal.portalUuid, regressionSplineChart)
//                }
//            }
//        }
    }

    fun updateCard(portal: SourcePortal, metricResult: ArtifactMetricResult, artifactMetrics: ArtifactMetrics) {
        val avg = artifactMetrics.values.average()
        val percents = calculatePercents(artifactMetrics)

        when (artifactMetrics.metricType) {
            Throughput_Average -> {
                val barTrendCard = BarTrendCard(
                    timeFrame = metricResult.timeFrame,
                    header = (avg / 60.0).fromPerSecondToPrettyFrequency(),
                    meta = artifactMetrics.metricType.toString().toLowerCase(),
                    barGraphData = percents
                )
                vertx.eventBus().displayCard(portal.portalUuid, barTrendCard)
            }
            ResponseTime_Average -> {
                val barTrendCard = BarTrendCard(
                    timeFrame = metricResult.timeFrame,
                    header = avg.toInt().toPrettyDuration(),
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
            else -> throw UnsupportedOperationException(artifactMetrics.metricType.name)
        }
    }

    private fun calculatePercents(artifactMetrics: ArtifactMetrics): List<Double> {
        val metricArr = ArrayList<Double>()
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
            if (percentMax == 0.0) {
                percents.add(0.0)
            } else {
                percents.add((metricArr[i] / percentMax) * 100.00)
            }
        }
        return percents
    }
}
