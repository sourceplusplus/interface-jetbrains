package com.sourceplusplus.protocol.artifact.metrics

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactSummarizedMetrics(
    val metricType: MetricType,
    val value: Double
)
