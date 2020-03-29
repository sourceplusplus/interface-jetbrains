package com.sourceplusplus.portal.display.tabs.views

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.portal.display.PortalInterface
import groovy.transform.Canonical

import java.util.concurrent.ConcurrentHashMap

import static com.sourceplusplus.api.model.metric.MetricType.ResponseTime_Average

/**
 * Holds the current view for the Overview portal tab.
 *
 * @version 0.2.4
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Canonical
class OverviewView {

    private final PortalInterface portalInterface
    private Map<String, Map<QueryTimeFrame, ArtifactMetricResult>> metricResultCache = new ConcurrentHashMap<>()
    QueryTimeFrame timeFrame = QueryTimeFrame.LAST_15_MINUTES
    MetricType activeChartMetric = ResponseTime_Average

    OverviewView(PortalInterface portalInterface) {
        this.portalInterface = portalInterface
    }

    void cloneView(OverviewView view) {
        metricResultCache = view.metricResultCache
        timeFrame = view.timeFrame
        activeChartMetric = view.activeChartMetric
    }

    void cacheMetricResult(ArtifactMetricResult metricResult) {
        metricResultCache.putIfAbsent(metricResult.artifactQualifiedName(), new ConcurrentHashMap<>())
        metricResultCache.get(metricResult.artifactQualifiedName()).put(metricResult.timeFrame(), metricResult)
    }

    ArtifactMetricResult getMetricResult() {
        return getMetricResult(portalInterface.viewingPortalArtifact, timeFrame)
    }

    ArtifactMetricResult getMetricResult(String artifactQualifiedName, QueryTimeFrame timeFrame) {
        return metricResultCache.get(artifactQualifiedName)?.get(timeFrame)
    }
}
