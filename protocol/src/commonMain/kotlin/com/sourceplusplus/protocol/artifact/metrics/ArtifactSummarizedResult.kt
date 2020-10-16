package com.sourceplusplus.protocol.artifact.metrics

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactSummarizedResult(
    val artifactQualifiedName: ArtifactQualifiedName,
    val artifactSummarizedMetrics: List<ArtifactSummarizedMetrics>
)
