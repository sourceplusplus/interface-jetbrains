package com.sourceplusplus.protocol.instrument.breakpoint.event

import com.sourceplusplus.protocol.Serializers
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.instrument.LiveInstrumentEventType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveBreakpointHit(
    val breakpointId: String,
    val traceId: String,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val occurredAt: Instant,
    val host: String,
    val application: String,
    val stackTrace: JvmStackTrace
) {
    val eventType: LiveInstrumentEventType = LiveInstrumentEventType.BREAKPOINT_HIT
}
