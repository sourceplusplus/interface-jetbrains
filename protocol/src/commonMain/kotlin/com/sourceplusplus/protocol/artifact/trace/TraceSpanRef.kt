package com.sourceplusplus.protocol.artifact.trace

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class TraceSpanRef(
    val traceId: String,
    val parentSegmentId: String,
    val parentSpanId: Int,
    val type: String
)
