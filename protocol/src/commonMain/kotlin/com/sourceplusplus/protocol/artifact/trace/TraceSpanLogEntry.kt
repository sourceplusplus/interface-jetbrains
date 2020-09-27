package com.sourceplusplus.protocol.artifact.trace

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class TraceSpanLogEntry(
    val time: Long, //todo: Instant
    val data: String
)
