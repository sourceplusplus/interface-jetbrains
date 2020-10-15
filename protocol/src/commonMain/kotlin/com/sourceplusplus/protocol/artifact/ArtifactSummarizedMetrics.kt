package com.sourceplusplus.protocol.artifact

import com.sourceplusplus.protocol.portal.MetricType
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactSummarizedMetrics(
    val metricType: MetricType,
    val value: String
)
