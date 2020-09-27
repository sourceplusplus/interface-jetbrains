package com.sourceplusplus.protocol.artifact.trace

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class InnerTraceStackInfo(
    val innerLevel: Int,
    val traceStack: String //todo: was JsonArray
)
