package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.mentor.task.*
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import io.vertx.core.Vertx

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RampDetectionMentor(
    override val vertx: Vertx
) : MentorJob() {

    override val tasks: List<MentorTask> = listOf(
        //get active service instance
        GetService(),
        GetServiceInstance(
            GetService.SERVICE
        ),

        GetEndpoints(),
        GetTraces(
            GetEndpoints.ENDPOINT_IDS,
            orderType = TraceOrderType.LATEST_TRACES,
            timeFrame = QueryTimeFrame.LAST_15_MINUTES
        ),

        //todo: ARIMA model?
        CalculateLinearRegression(GetTraces.TRACE_RESULT),
        //DelayTask(10_000)

        //todo: find endpoints with consistently increasing response time of a certain threshold
        //todo: search source code of endpoint for culprits
    )
}