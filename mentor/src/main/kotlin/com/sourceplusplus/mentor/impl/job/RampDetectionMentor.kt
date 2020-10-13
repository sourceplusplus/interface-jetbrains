package com.sourceplusplus.mentor.impl.job

import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.extend.SqlProducerSearch
import com.sourceplusplus.mentor.impl.task.analyze.CalculateLinearRegression
import com.sourceplusplus.mentor.impl.task.analyze.DetermineTraceSpanArtifact
import com.sourceplusplus.mentor.impl.task.filter.FilterBoundedDatabaseQueries
import com.sourceplusplus.mentor.impl.task.filter.FilterTraceStacks
import com.sourceplusplus.mentor.impl.task.general.CreateArtifactAdvice
import com.sourceplusplus.mentor.impl.task.general.DelayTask
import com.sourceplusplus.mentor.impl.task.monitor.*
import com.sourceplusplus.protocol.advice.AdviceType
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceSpanQuery
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import io.vertx.core.Vertx

/**
 * Keeps track of endpoint durations for indications of 'The Ramp' [Smith and Williams 2002] performance anti-pattern.
 * Uses a continuous linear regression model which requires a specific confidence before alerting developer.
 * May also indicate root cause by searching trace stack for probable offenders.
 *
 * [Smith and Williams 2002] C. U. Smith and L. G. Williams, Performance Solutions: A Practical Guide to
 * Creating Responsive, Scalable Software, Boston, MA, Addison-Wesley, 2002.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class RampDetectionMentor(
    override val vertx: Vertx,
    sqlProducerSearch: SqlProducerSearch,
    confidence: Double = 0.5
) : MentorJob() {

    override val tasks: List<MentorTask> by lazy {
        listOfNotNull(
            //get active service instance
            GetService(),
            GetServiceInstance(GetService.SERVICE),

            //iterate endpoints (checking likely offenders more frequently than non-likely offenders)
            GetEndpoints(),
            GetTraces(
                GetEndpoints.ENDPOINT_IDS,
                orderType = TraceOrderType.LATEST_TRACES,
                timeFrame = QueryTimeFrame.LAST_15_MINUTES
            ),

            //keep track of regression of endpoint duration
            CalculateLinearRegression(GetTraces.TRACE_RESULT, confidence), //todo: ARIMA model?

            //get trace stack for regressive endpoints
            GetTraceStacks(byTracesContext = CalculateLinearRegression.TRACES),

            //filter down by obvious regressive features
            FilterTraceStacks(
                GetTraceStacks.TRACE_STACKS,
                filterQuery = TraceSpanQuery(tags = setOf("db.statement"))
            ),
            FilterBoundedDatabaseQueries(FilterTraceStacks.TRACE_SPANS),

            DetermineTraceSpanArtifact(
                FilterBoundedDatabaseQueries.TRACE_SPANS,
                sqlProducerSearch = sqlProducerSearch
            ),

            //create RampDetectionAdvice
            CreateArtifactAdvice(
                byArtifactsContext = DetermineTraceSpanArtifact.ARTIFACTS,
                adviceType = AdviceType.RampDetectionAdvice
            ),

            if (config.repeatForever) {
                DelayTask(config.repeatDelay)
            } else null //todo: should likely move delay logic to SourceMentor
        )
    }
}