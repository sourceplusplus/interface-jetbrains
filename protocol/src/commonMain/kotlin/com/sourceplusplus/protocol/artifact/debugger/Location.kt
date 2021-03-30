package com.sourceplusplus.protocol.artifact.debugger

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class Location(
    val source: String,
    val line: Int
)
