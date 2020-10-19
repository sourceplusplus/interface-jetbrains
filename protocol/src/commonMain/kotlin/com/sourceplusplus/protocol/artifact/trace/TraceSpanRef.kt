package com.sourceplusplus.protocol.artifact.trace

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class TraceSpanRef(
    val traceId: String,
    val parentSegmentId: String,
    val parentSpanId: Int,
    val type: String
)
