package com.sourceplusplus.portal.display.views

import com.sourceplusplus.portal.SourcePortal
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.metrics.ArtifactMetricResult
import spp.protocol.artifact.metrics.ArtifactMetrics
import spp.protocol.artifact.metrics.MetricType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaInstant
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the current view for the Activity portal tab.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActivityView(
    val portal: SourcePortal
) {

    companion object {
        private val log = LoggerFactory.getLogger(ActivityView::class.java)
    }

    var metricResultCache = ConcurrentHashMap<String, MutableMap<QueryTimeFrame, ArtifactMetricResult>>()
    var timeFrame = QueryTimeFrame.LAST_5_MINUTES
    var activeChartMetric = MetricType.ResponseTime_Average

    fun cloneView(view: ActivityView) {
        metricResultCache = view.metricResultCache
        timeFrame = view.timeFrame
        activeChartMetric = view.activeChartMetric
    }

    fun cacheMetricResult(metricResult: ArtifactMetricResult) {
        metricResultCache.putIfAbsent(
            metricResult.artifactQualifiedName,
            ConcurrentHashMap<QueryTimeFrame, ArtifactMetricResult>()
        )

        if (metricResult.pushMode) {
            if (!metricResultCache[metricResult.artifactQualifiedName]!!.containsKey(metricResult.timeFrame)) {
                metricResultCache[metricResult.artifactQualifiedName]!![metricResult.timeFrame] = metricResult
            }
            metricResultCache[metricResult.artifactQualifiedName]!!.forEach { (_, cachedResult) ->
                if (cachedResult.start <= metricResult.start && cachedResult.stop >= metricResult.stop) {
                    //log.info("Updating artifact value at: {} - {}", metricResult.start, metricResult.stop)
                    val updateIndex = ChronoUnit.MINUTES.between(
                        cachedResult.start.toJavaInstant(), metricResult.start.toJavaInstant()
                    ).toInt()
                    val updatedMetrics = mutableListOf<ArtifactMetrics>()
                    cachedResult.artifactMetrics.forEach { metric ->
                        val updatedMetric = metricResult.artifactMetrics.find { metric.metricType == it.metricType }
                        if (updatedMetric != null) {
                            updatedMetrics.add(
                                ArtifactMetrics(
                                    metric.metricType,
                                    metric.values.toMutableList()
                                        .apply { set(updateIndex, updatedMetric.values[0]) }
                                )
                            )
                        }
                    }

                    val newMetrics = cachedResult.artifactMetrics.toMutableList()
                        .filterNot { updatedMetrics.map { it.metricType }.contains(it.metricType) }
                        .toMutableList().apply { addAll(updatedMetrics) }
                    metricResultCache[cachedResult.artifactQualifiedName]!![cachedResult.timeFrame] =
                        cachedResult.copy(artifactMetrics = newMetrics)
                } else if (cachedResult.stop <= metricResult.start) {
                    val moveIndex = ChronoUnit.MINUTES.between(
                        cachedResult.stop.toJavaInstant(), metricResult.start.toJavaInstant()
                    ).toInt() + 1
                    val updatedMetrics = mutableListOf<ArtifactMetrics>()
                    cachedResult.artifactMetrics.forEach { metric ->
                        val updatedMetric = metricResult.artifactMetrics.find { metric.metricType == it.metricType }
                        if (updatedMetric != null) {
                            updatedMetrics.add(
                                ArtifactMetrics(
                                    metric.metricType,
                                    metric.values.toMutableList().apply { addAll(updatedMetric.values) }
                                        .drop(moveIndex)
                                )
                            )
                        }
                    }

                    val newMetrics = cachedResult.artifactMetrics.toMutableList()
                        .filterNot { updatedMetrics.map { it.metricType }.contains(it.metricType) }
                        .toMutableList().apply { addAll(updatedMetrics) }
                    metricResultCache[cachedResult.artifactQualifiedName]!![cachedResult.timeFrame] =
                        cachedResult.copy(
                            start = cachedResult.start.plus(moveIndex, DateTimeUnit.MINUTE),
                            stop = cachedResult.stop.plus(moveIndex, DateTimeUnit.MINUTE),
                            artifactMetrics = newMetrics
                        )
                }
            }
        } else {
            metricResultCache[metricResult.artifactQualifiedName]!![metricResult.timeFrame] = metricResult
        }
    }

    val metricResult: ArtifactMetricResult?
        get() = getMetricResult(portal.viewingPortalArtifact, timeFrame)

    fun getMetricResult(artifactQualifiedName: String, timeFrame: QueryTimeFrame): ArtifactMetricResult? {
        return metricResultCache[artifactQualifiedName]?.get(timeFrame)
    }
}
