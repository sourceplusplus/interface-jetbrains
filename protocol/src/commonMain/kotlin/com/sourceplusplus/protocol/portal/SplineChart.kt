package com.sourceplusplus.protocol.portal

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class SplineChart(
    val metricType: MetricType,
    val timeFrame: QueryTimeFrame, //todo: use LocalDuration
    val seriesData: List<SplineSeriesData>
)
