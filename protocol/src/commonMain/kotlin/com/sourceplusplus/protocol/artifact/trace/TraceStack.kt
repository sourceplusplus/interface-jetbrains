package com.sourceplusplus.protocol.artifact.trace

import com.sourceplusplus.protocol.artifact.trace.TraceStack.Segment
import kotlinx.serialization.Serializable

/**
 * Represents a list of [TraceSpan]s as the equivalent tree structure.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class TraceStack(val traceSpans: List<TraceSpan>) : Iterable<Segment> {
    private val segmentMap: MutableMap<String, Segment> = mutableMapOf()
    val isEmpty: Boolean = traceSpans.isEmpty()
    fun size(): Int = traceSpans.size
    val segments: Int

    init {
        for ((segmentId, traceSpans) in traceSpans.groupBy { it.segmentId }) {
            val segment = Segment(segmentId, traceSpans, traceSpans.distinctBy { it.parentSpanId }.size)
            segmentMap[segmentId] = segment
            for (traceSpan in traceSpans) {
                segment.spanMap[traceSpan.spanId] = traceSpan
                val parent = getParent(traceSpan)
                if (parent != null) {
                    if (!segment.childSpanMap.containsKey(parent)) {
                        segment.childSpanMap[parent] = mutableListOf()
                    }
                    segment.childSpanMap[parent]!!.add(traceSpan)
                }
            }
        }
        segments = segmentMap.size
    }

    fun hasParent(traceSpan: TraceSpan): Boolean {
        return getParent(traceSpan) == null
    }

    fun getParent(traceSpan: TraceSpan): TraceSpan? {
        return getSegment(traceSpan).spanMap[traceSpan.parentSpanId]
    }

    fun hasChildren(traceSpan: TraceSpan): Boolean {
        return getChildren(traceSpan).isNotEmpty()
    }

    fun getChildren(traceSpan: TraceSpan): List<TraceSpan> {
        return getSegment(traceSpan).getChildren(traceSpan)
    }

    fun getSegment(segmentId: String): Segment {
        return segmentMap[segmentId]!!
    }

    fun getSegment(traceSpan: TraceSpan): Segment {
        return segmentMap[traceSpan.segmentId]!!
    }

    fun getSegmentIds(): List<String> {
        return segmentMap.keys.toList()
    }

    override fun iterator(): Iterator<Segment> {
        return segmentMap.values.iterator()
    }

    @Serializable
    data class Segment(
        val segmentId: String,
        val traceSpans: List<TraceSpan>,
        val depth: Int = 0
    ) : Collection<TraceSpan> {
        internal val spanMap: MutableMap<Int, TraceSpan> = mutableMapOf()
        internal val childSpanMap: MutableMap<TraceSpan, MutableList<TraceSpan>> = mutableMapOf()

        fun getTraceSpan(spanId: Int): TraceSpan {
            return spanMap[spanId]!!
        }

        fun getParent(spanId: Int): TraceSpan? {
            return spanMap[getTraceSpan(spanId).parentSpanId]
        }

        fun getParent(traceSpan: TraceSpan): TraceSpan? {
            return spanMap[traceSpan.parentSpanId]
        }

        fun hasChildren(traceSpan: TraceSpan): Boolean {
            return getChildren(traceSpan).isNotEmpty()
        }

        fun getChildren(spanId: Int): List<TraceSpan> {
            return childSpanMap[getTraceSpan(spanId)]?.toList() ?: emptyList()
        }

        fun getChildren(traceSpan: TraceSpan): List<TraceSpan> {
            return childSpanMap[traceSpan]?.toList() ?: emptyList()
        }

        override val size: Int = traceSpans.size
        override fun contains(element: TraceSpan): Boolean = traceSpans.contains(element)
        override fun containsAll(elements: Collection<TraceSpan>): Boolean = traceSpans.containsAll(elements)
        override fun isEmpty(): Boolean = traceSpans.isEmpty()
        override fun iterator(): Iterator<TraceSpan> = traceSpans.iterator()
    }
}
