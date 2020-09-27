package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.mentor.task.DetermineThrowableLocation
import com.sourceplusplus.mentor.task.GetService
import com.sourceplusplus.mentor.task.GetServiceInstance
import com.sourceplusplus.mentor.task.GetTraces
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import io.vertx.core.Vertx

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActiveExceptionMentor(
    override val vertx: Vertx
) : MentorJob() {

    override val tasks: List<MentorTask> = listOf(
        //get active service instance
        GetService(),
        GetServiceInstance(
            GetService.SERVICE
        ),

        //fetch failing traces
        GetTraces(
            TraceOrderType.FAILED_TRACES,
            QueryTimeFrame.LAST_15_MINUTES
        ),

        //search failing traces to determine failing source code location
        DetermineThrowableLocation(
            GetTraces.TRACES
        )

        //todo: create advice
        //todo: maintain created advice status (remove on new instances, etc)
    )
}