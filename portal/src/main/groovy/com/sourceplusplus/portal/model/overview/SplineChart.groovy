package com.sourceplusplus.portal.model.overview

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.metric.MetricType
import groovy.transform.builder.Builder

/**
 * Represents an artifact's spline chart data.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Builder
class SplineChart {
    MetricType metricType
    QueryTimeFrame timeFrame
    List<SplineSeriesData> seriesData
}
