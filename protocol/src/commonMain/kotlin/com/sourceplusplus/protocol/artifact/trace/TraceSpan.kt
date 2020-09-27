package com.sourceplusplus.protocol.artifact.trace

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
//todo: remove ?s
data class TraceSpan(
    val traceId: String? = null,
    val segmentId: String,
    val spanId: Int? = null,
    val parentSpanId: Int? = null,
    val refs: List<TraceSpanRef> = emptyList(),
    val serviceCode: String? = null,
    val serviceInstanceName: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val endpointName: String? = null,
    val artifactQualifiedName: String? = null,
    val type: String? = null,
    val peer: String? = null,
    val component: String? = null,
    val error: Boolean? = null,
    val childError: Boolean = false,
    val hasChildStack: Boolean? = null,
    val layer: String? = null,
    val tags: Map<String, String> = emptyMap(),
    val logs: List<TraceSpanLogEntry> = emptyList()
)
