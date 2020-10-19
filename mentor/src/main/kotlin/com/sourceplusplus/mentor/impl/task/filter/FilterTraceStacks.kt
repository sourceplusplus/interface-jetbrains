package com.sourceplusplus.mentor.impl.task.filter

import com.sourceplusplus.mentor.base.ContextKey
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.trace.TraceSpanQuery
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class FilterTraceStacks(
    private val byTraceStacksContext: ContextKey<List<TraceSpanStackQueryResult>>,
    private val filterQuery: TraceSpanQuery
) : MentorTask() {

    companion object {
        val TRACE_SPANS: ContextKey<List<TraceSpan>> = ContextKey("FilterTraceStacks.TRACE_SPANS")
    }

    override val inputContextKeys: List<ContextKey<*>> = listOfNotNull(byTraceStacksContext)
    override val outputContextKeys = listOf(TRACE_SPANS)

    override suspend fun executeTask(job: MentorJob) {
        val filteredSpans = mutableListOf<TraceSpan>()

        val traceStacks = job.context.get(byTraceStacksContext)
        traceStacks.forEach { traceStack ->
            traceStack.traceSpans.forEach { traceSpan ->
                //todo: rest of filter
                if (filterQuery.tags.all { traceSpan.tags.containsKey(it) && !traceSpan.tags[it].isNullOrEmpty() }) {
                    filteredSpans.add(traceSpan)
                }
            }
        }

        job.context.put(TRACE_SPANS, filteredSpans)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilterTraceStacks) return false
        if (byTraceStacksContext != other.byTraceStacksContext) return false
        if (filterQuery != other.filterQuery) return false
        return true
    }

    override fun hashCode(): Int {
        var result = byTraceStacksContext.hashCode()
        result = 31 * result + filterQuery.hashCode()
        return result
    }
}
