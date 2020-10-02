package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.track.EndpointTracesTracker
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GetTraceStacks(
    private val byTracesContext: MentorJob.ContextKey<TraceResult>,
    private val distinctByOperationName: Boolean = true //todo: impl
) : MentorTask() {

    companion object {
        val TRACE_STACKS: MentorJob.ContextKey<List<TraceSpanStackQueryResult>> =
            MentorJob.ContextKey("GetTraceStacks.TRACE_STACKS")
    }

    override val contextKeys = listOf(TRACE_STACKS)

    override suspend fun executeTask(job: MentorJob) {
        job.log(
            "Task configuration\n\t" +
                    "byTracesContext: $byTracesContext\n\t" +
                    "distinctByOperationName: $distinctByOperationName"
        )

        val traceResult = job.context.get(byTracesContext)
        val traceStacks = mutableListOf<TraceSpanStackQueryResult>()
        traceResult.traces.distinctBy { it.operationNames }.forEach { trace ->
            val traceStack = EndpointTracesTracker.getTraceStack(trace.traceIds[0], job.vertx)
            traceStacks.add(traceStack)
        }
        job.context.put(TRACE_STACKS, traceStacks)
        job.log("Added context\n\tKey: $TRACE_STACKS\n\tSize: ${traceStacks.size}")
    }
}