package com.sourceplusplus.protocol.artifact.debugger.event

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
enum class BreakpointEventType {
    HIT,
    REMOVED
}
