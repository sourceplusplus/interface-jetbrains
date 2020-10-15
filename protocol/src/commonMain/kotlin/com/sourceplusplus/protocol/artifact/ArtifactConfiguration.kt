package com.sourceplusplus.protocol.artifact

import kotlinx.serialization.Serializable

@Serializable
data class ArtifactConfiguration(
    val endpoint: Boolean,
    val subscribeAutomatically: Boolean,
    val endpointName: String,
    val endpointIds: List<Long>
)