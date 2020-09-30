package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.model.GetEndpointTraces
import com.sourceplusplus.monitor.skywalking.model.ZonedDuration
import com.sourceplusplus.monitor.skywalking.track.EndpointTracesTracker
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class GetTraces(
    private val orderType: TraceOrderType,
    private val timeFrame: QueryTimeFrame, //todo: impl start/end in QueryTimeFrame
    private val limit: Int = 100,
    private val haltOnEmptyTraces: Boolean = true
) : MentorTask() {

    companion object {
        val TRACE_RESULT: ContextKey<TraceResult> = ContextKey()
    }

    override val contextKeys = listOf(TRACE_RESULT)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Executing task: $this")

        val traces = EndpointTracesTracker.getTraces(
            GetEndpointTraces(
                appUuid = "null", //todo: likely not necessary
                artifactQualifiedName = "null", //todo: likely not necessary
                orderType = orderType,
                zonedDuration = ZonedDuration( //todo: use timeFrame
                    ZonedDateTime.now().minusMinutes(15),
                    ZonedDateTime.now(),
                    SkywalkingClient.DurationStep.MINUTE
                )
            ), job.vertx
        )

        if (haltOnEmptyTraces && traces.traces.isEmpty()) {
            job.complete()
        } else {
            job.context.put(TRACE_RESULT, traces)
        }
    }
}
