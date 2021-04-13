package com.sourceplusplus.protocol.artifact.debugger.event

import com.sourceplusplus.protocol.Serializers
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class BreakpointHit(
    val breakpointId: String,
    val traceId: String,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val occurredAt: Instant,
    val host: String,
    val application: String,
    val stackTrace: JvmStackTrace
)
