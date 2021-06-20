package com.sourceplusplus.protocol.instrument.log.event

import com.sourceplusplus.protocol.Serializers
import com.sourceplusplus.protocol.artifact.log.LogResult
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
data class LiveLogHit(
    val logId: String,
    @Serializable(with = Serializers.InstantKSerializer::class)
    val occurredAt: Instant,
    val serviceInstance: String,
    val service: String,
    val logResult: LogResult
) {
    val eventType: LiveInstrumentEventType = LiveInstrumentEventType.LOG_HIT
}
