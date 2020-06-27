package com.sourceplusplus.portal.model.overview

import com.sourceplusplus.api.model.QueryTimeFrame
import com.sourceplusplus.api.model.metric.MetricType
import groovy.transform.builder.Builder

/**
 * Represents an artifact's spline chart data.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.1.0
 */
@Builder
class SplineChart {
    MetricType metricType
    QueryTimeFrame timeFrame
    List<SplineSeriesData> seriesData
}
