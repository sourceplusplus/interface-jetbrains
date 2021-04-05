package com.sourceplusplus.protocol.artifact.debugger

import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import kotlinx.datetime.Instant

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class BreakpointHit(
    val breakpointId: String,
    val occurredAt: Instant,
    val host: String,
    val application: String,
    val stackTrace: JvmStackTrace
)
