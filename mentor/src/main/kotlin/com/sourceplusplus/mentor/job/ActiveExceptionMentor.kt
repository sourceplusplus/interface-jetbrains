package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.mentor.task.analyze.DetermineThrowableLocation
import com.sourceplusplus.mentor.task.general.DelayTask
import com.sourceplusplus.mentor.task.monitor.GetService
import com.sourceplusplus.mentor.task.monitor.GetServiceInstance
import com.sourceplusplus.mentor.task.monitor.GetTraceStacks
import com.sourceplusplus.mentor.task.monitor.GetTraces
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import io.vertx.core.Vertx

/**
 * Searches failed traces to determine root cause source code location.
 *
 * Todo: maintain created advice status (remove on new instances, etc)
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActiveExceptionMentor(
    override val vertx: Vertx,
    userRootSourcePackage: String
) : MentorJob() {

    override val tasks: List<MentorTask> by lazy {
        listOfNotNull(
            //get active service instance
            GetService(),
            GetServiceInstance(
                GetService.SERVICE
            ),

            //fetch failed traces
            GetTraces(
                orderType = TraceOrderType.FAILED_TRACES,
                timeFrame = QueryTimeFrame.LAST_15_MINUTES
            ),
            GetTraceStacks(GetTraces.TRACE_RESULT),

            //search failed traces to determine throwable source code location
            DetermineThrowableLocation(
                GetTraceStacks.TRACE_STACKS,
                userRootSourcePackage
            ),

            if (config.repeatForever) {
                DelayTask(config.repeatDelay)
            } else null
        )
    }
}