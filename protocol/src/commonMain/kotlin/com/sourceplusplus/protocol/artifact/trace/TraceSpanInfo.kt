package com.sourceplusplus.protocol.artifact.trace

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class TraceSpanInfo(
    val span: TraceSpan,
    val timeTook: String,
    val appUuid: String,
    val rootArtifactQualifiedName: String,
    val operationName: String? = null,
    val totalTracePercent: Double
)
