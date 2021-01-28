package com.sourceplusplus.protocol.artifact.log

import com.sourceplusplus.protocol.Serializers
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class Log (
    @Serializable(with = Serializers.InstantKSerializer::class)
    val timestamp: Instant,
    val content: String
)