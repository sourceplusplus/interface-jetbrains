package com.sourceplusplus.protocol.artifact.trace

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class TraceSpanInfo(
    val span: TraceSpan,
    val timeTook: String,
    val appUuid: String,
    val rootArtifactQualifiedName: String,
    val operationName: String? = null,
    val totalTracePercent: Double,
    val innerLevel: Int = 0
)
