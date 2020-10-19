package com.sourceplusplus.protocol.artifact.trace

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
data class TraceSpanLogEntry(
    @Serializable(with = Serializers.InstantKSerializer::class)
    val time: Instant,
    val data: String
)
