package com.sourceplusplus.protocol.instrument.log.event

import com.sourceplusplus.protocol.Serializers
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.instrument.LiveInstrumentEventType
import com.sourceplusplus.protocol.instrument.TrackedLiveEvent
import com.sourceplusplus.protocol.instrument.log.LiveLog
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveLogRemoved(
    val logId: String,
    @Serializable(with = Serializers.InstantKSerializer::class)
    override val occurredAt: Instant,
    val cause: JvmStackTrace? = null,
    val liveLog: LiveLog
) : TrackedLiveEvent {
    val eventType: LiveInstrumentEventType = LiveInstrumentEventType.LOG_REMOVED
}
