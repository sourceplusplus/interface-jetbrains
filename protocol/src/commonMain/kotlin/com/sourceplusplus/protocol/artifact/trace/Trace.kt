package com.sourceplusplus.protocol.artifact.trace

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class Trace(
    val key: String? = null,
    val operationNames: List<String>,
    val duration: Int,
    val start: Long,
    val error: Boolean? = null,
    val traceIds: List<String>,
    val prettyDuration: String? = null,
    val partial: Boolean = false,
    val segmentId: String? = null
)
