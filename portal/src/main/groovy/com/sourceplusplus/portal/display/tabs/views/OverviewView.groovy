package com.sourceplusplus.portal.display.tabs.views

import com.sourceplusplus.api.model.metric.ArtifactMetricResult

import java.util.concurrent.ConcurrentHashMap

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class OverviewView {

    private final Map<String, ArtifactMetricResult> metricResultCache = new ConcurrentHashMap<>()

    Map<String, ArtifactMetricResult> getMetricResultCache() {
        return metricResultCache
    }
}
