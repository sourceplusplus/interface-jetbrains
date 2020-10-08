package com.sourceplusplus.mentor.impl.task.general

import com.sourceplusplus.mentor.base.ContextKey
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.impl.task.analyze.CalculateLinearRegression
import com.sourceplusplus.mentor.impl.task.analyze.CalculateLinearRegression.Companion.REGRESSION_MAP
import com.sourceplusplus.mentor.impl.task.monitor.GetTraces
import com.sourceplusplus.protocol.advice.AdviceType
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.advice.cautionary.RampDetectionAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.trace.TraceSpan

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class CreateArtifactAdvice(
    private val byTraceSpansContext: ContextKey<List<TraceSpan>>? = null,
    private val byArtifactAdviceContext: ContextKey<List<ArtifactAdvice>>? = null,
    adviceType: AdviceType
) : MentorTask() {

    override val inputContextKeys: List<ContextKey<*>> = listOfNotNull(byTraceSpansContext)

    override suspend fun executeTask(job: MentorJob) {
        if (byArtifactAdviceContext != null) {
            job.context.get(byArtifactAdviceContext).forEach {
                job.addAdvice(it)
            }
        } else {
            val traceSpans = job.context.get(byTraceSpansContext!!)
            traceSpans.forEach { traceSpan ->
                val fullTrace = job.context.get(GetTraces.TRACE_RESULT).traces.find {
                    it.traceIds[0] == traceSpan.traceId
                }!! //todo: cannot assume GetTraces.TRACE_RESULT exists
                val regression = job.context.get(REGRESSION_MAP)[fullTrace.operationNames[0]]!! //todo: or this
                job.addAdvice(
                    RampDetectionAdvice(
                        ArtifactQualifiedName(fullTrace.operationNames[0], "todo", ArtifactType.ENDPOINT),
                        CalculateLinearRegression.ApacheSimpleRegression(regression)
                    ) //todo: switch on advice type
                )
            }
        }
    }
}
