package com.sourceplusplus.protocol.artifact

import com.sourceplusplus.protocol.Serializers
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactInformation(
    val artifactQualifiedName: String,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val createDate: Instant,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val lastUpdated: Instant,
    val config: ArtifactConfiguration
)
