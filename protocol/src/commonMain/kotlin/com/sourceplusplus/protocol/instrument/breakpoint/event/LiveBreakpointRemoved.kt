package com.sourceplusplus.protocol.instrument.breakpoint.event

import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import com.sourceplusplus.protocol.instrument.LiveInstrumentEventType
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveBreakpointRemoved(
    val breakpointId: String,
    val cause: JvmStackTrace? = null
) {
    val eventType: LiveInstrumentEventType = LiveInstrumentEventType.BREAKPOINT_REMOVED
}
