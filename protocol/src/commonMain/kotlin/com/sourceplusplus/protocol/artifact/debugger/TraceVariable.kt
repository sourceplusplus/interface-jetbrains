package com.sourceplusplus.protocol.artifact.debugger

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class TraceVariable(
    val name: String,
    @Contextual val value: Any,
    val lineNumber: Int = -1
)
