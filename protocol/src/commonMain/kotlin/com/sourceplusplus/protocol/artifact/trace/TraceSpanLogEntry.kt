package com.sourceplusplus.protocol.artifact.trace

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class TraceSpanLogEntry(
    val time: Long, //todo: Instant
    val data: String
)
