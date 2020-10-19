package com.sourceplusplus.protocol.artifact

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactConfiguration(
    val endpoint: Boolean,
    val subscribeAutomatically: Boolean,
    val endpointName: String,
    val endpointIds: List<Long> = emptyList()
)
