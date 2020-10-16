package com.sourceplusplus.protocol.artifact.metrics

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactMetrics(
    val metricType: MetricType,
    val values: List<Int>
)
