package com.sourceplusplus.protocol.artifact.debugger.event

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class BreakpointEvent(
    val eventType: BreakpointEventType,
    val data: String
)
