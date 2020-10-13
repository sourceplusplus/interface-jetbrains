package com.sourceplusplus.protocol.artifact.endpoint

import com.sourceplusplus.protocol.artifact.ArtifactMetricResult
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class EndpointResult(
    val endpointMetrics: List<ArtifactMetricResult>
)
