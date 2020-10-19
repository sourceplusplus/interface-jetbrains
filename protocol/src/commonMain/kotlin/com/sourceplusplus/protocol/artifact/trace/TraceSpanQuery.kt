package com.sourceplusplus.protocol.artifact.trace

import kotlinx.datetime.Instant

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class TraceSpanQuery(
    val traceId: String? = null,
    val segmentId: String? = null,
    val spanId: Int? = null,
    val parentSpanId: Int? = null,
//    val refs: List<TraceSpanRef> = emptyList(),
    val serviceCode: String? = null,
    val serviceInstanceName: String? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val endpointName: String? = null,
    val artifactQualifiedName: String? = null,
    val type: String? = null,
    val peer: String? = null,
    val component: String? = null,
    val error: Boolean? = null,
    val childError: Boolean? = null,
    val hasChildStack: Boolean? = null,
    val layer: String? = null,
    val tags: Set<String> = emptySet(),
//    val logs: List<TraceSpanLogEntry> = emptyList()
)
