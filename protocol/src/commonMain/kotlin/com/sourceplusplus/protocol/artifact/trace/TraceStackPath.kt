package com.sourceplusplus.protocol.artifact.trace

import kotlinx.serialization.Serializable

/**
 * Used to keep track of the path taken in a [TraceStack].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class TraceStackPath(
    val traceStack: TraceStack,
    val path: List<TraceSpan> = mutableListOf(),
    val orderType: TraceOrderType
) {

    fun getCurrentSegment(): TraceStack.Segment? {
        val segmentId = path.lastOrNull()?.segmentId
        return if (segmentId != null) {
            traceStack.getSegment(segmentId)
        } else null
    }

    fun getCurrentRoot(): TraceSpan? {
        return path.lastOrNull()
    }

    fun follow(segmentId: String, spanId: Int) {
        (path as MutableList).add(traceStack.getSegment(segmentId).getTraceSpan(spanId))
    }

    fun removeLastRoot() {
        (path as MutableList).removeLast()
    }
}
