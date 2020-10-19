package com.sourceplusplus.portal.display.views

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.metrics.MetricType
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
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
        metricResultCache[metricResult.artifactQualifiedName]!![metricResult.timeFrame] = metricResult
    }

    val metricResult: ArtifactMetricResult?
        get() = getMetricResult(portal.viewingPortalArtifact, timeFrame)

    fun getMetricResult(artifactQualifiedName: String, timeFrame: QueryTimeFrame): ArtifactMetricResult? {
        return metricResultCache[artifactQualifiedName]?.get(timeFrame)
    }
}
