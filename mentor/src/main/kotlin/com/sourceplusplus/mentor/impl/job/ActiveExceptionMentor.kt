package com.sourceplusplus.mentor.impl.job

import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.impl.task.analyze.DetermineThrowableLocation
import com.sourceplusplus.mentor.impl.task.general.CreateArtifactAdvice
import com.sourceplusplus.mentor.impl.task.general.DelayTask
import com.sourceplusplus.mentor.impl.task.monitor.GetService
import com.sourceplusplus.mentor.impl.task.monitor.GetServiceInstance
import com.sourceplusplus.mentor.impl.task.monitor.GetTraceStacks
import com.sourceplusplus.mentor.impl.task.monitor.GetTraces
import com.sourceplusplus.protocol.advice.AdviceType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import io.vertx.core.Vertx

/**
 * Searches failed traces to determine root cause source code location.
 *
 * Todo: maintain created advice status (remove on new instances, etc)
 *
 * @since 0.1.0
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
            GetServiceInstance(GetService.SERVICE),

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

            //create ActiveExceptionAdvice
            CreateArtifactAdvice(
                byArtifactAdviceContext = DetermineThrowableLocation.ARTIFACT_ADVICE,
                adviceType = AdviceType.ActiveExceptionAdvice
            ),

            if (config.repeatForever) {
                DelayTask(config.repeatDelay)
            } else null
        )
    }
}