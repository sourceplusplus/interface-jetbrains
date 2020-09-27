package com.sourceplusplus.protocol.artifact

import com.sourceplusplus.protocol.portal.MetricType

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class ArtifactMetrics(
    val metricType: MetricType,
    val values: List<Int>
)
