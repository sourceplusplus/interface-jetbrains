package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.model.GetEndpointTraces
import com.sourceplusplus.monitor.skywalking.model.ZonedDuration
import com.sourceplusplus.monitor.skywalking.track.EndpointTracesTracker
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
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
        val TRACES: ContextKey<Nothing> = ContextKey()
    }

    override suspend fun executeTask(job: MentorJob, context: TaskContext) {

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

        TODO("Not yet implemented")
    }
}
