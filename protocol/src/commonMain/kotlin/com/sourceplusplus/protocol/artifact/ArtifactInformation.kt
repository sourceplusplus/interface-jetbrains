package com.sourceplusplus.protocol.artifact

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactInformation(
    val artifactQualifiedName: String,
    val createDate: Long,
    val lastUpdated: Long,
    val config: ArtifactConfiguration
)
