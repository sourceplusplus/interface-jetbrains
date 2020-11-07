package com.sourceplusplus.mentor.impl.task.monitor

import com.sourceplusplus.mentor.base.ContextKey
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.monitor.skywalking.bridge.EndpointTracesBridge
import com.sourceplusplus.protocol.artifact.trace.Trace
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GetTraceStacks(
    private val byTraceResultContext: ContextKey<TraceResult>? = null,
    private val byTracesContext: ContextKey<List<Trace>>? = null,
    private val distinctByOperationName: Boolean = true
) : MentorTask() {

    companion object {
        val TRACE_STACKS: ContextKey<List<TraceSpanStackQueryResult>> = ContextKey("GetTraceStacks.TRACE_STACKS")
    }

    override val outputContextKeys = listOf(TRACE_STACKS)

    override suspend fun executeTask(job: MentorJob) {
        job.log(
            "Task configuration\n\t" +
                    "byTraceResultContext: $byTraceResultContext\n\t" +
                    "byTracesContext: $byTracesContext\n\t" +
                    "distinctByOperationName: $distinctByOperationName"
        )

        val traceStacks = mutableListOf<TraceSpanStackQueryResult>()
        var traces = if (byTraceResultContext != null) {
            job.context.get(byTraceResultContext).traces
        } else {
            job.context.get(byTracesContext!!)
        }
        if (distinctByOperationName) {
            traces = traces.distinctBy { it.operationNames }
        }
        traces.forEach { trace ->
            val traceStack = EndpointTracesBridge.getTraceStack(trace.traceIds[0], job.vertx)
            traceStacks.add(traceStack)
        }

        job.context.put(TRACE_STACKS, traceStacks)
        job.log("Added context\n\tKey: $TRACE_STACKS\n\tSize: ${traceStacks.size}")
    }
}