package com.sourceplusplus.protocol.artifact

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LocalArtifact(
    val artifactQualifiedName: ArtifactQualifiedName,
    val filePath: String
)
