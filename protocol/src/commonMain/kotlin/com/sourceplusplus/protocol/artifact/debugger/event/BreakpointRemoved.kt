package com.sourceplusplus.protocol.artifact.debugger.event

import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class BreakpointRemoved(
    val breakpointId: String,
    val cause: JvmStackTrace? = null
)
