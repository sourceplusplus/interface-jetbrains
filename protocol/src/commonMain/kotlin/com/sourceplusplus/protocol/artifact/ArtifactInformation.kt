package com.sourceplusplus.protocol.artifact

import kotlinx.serialization.Serializable

@Serializable
data class ArtifactInformation(
    val artifactQualifiedName: String,
    val createDate: Long,
    val lastUpdated: Long,
    val config: ArtifactConfiguration
)
