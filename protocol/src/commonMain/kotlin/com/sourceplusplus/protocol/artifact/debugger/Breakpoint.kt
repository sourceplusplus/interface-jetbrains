package com.sourceplusplus.protocol.artifact.debugger

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class Breakpoint(
    val location: Location,
    val condition: String? = null,
    val id: String? = null
)
