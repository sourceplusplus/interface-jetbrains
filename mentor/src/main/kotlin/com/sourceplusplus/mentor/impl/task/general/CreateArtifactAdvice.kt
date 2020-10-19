package com.sourceplusplus.mentor.impl.task.general

import com.sourceplusplus.mentor.base.ContextKey
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.impl.task.analyze.CalculateLinearRegression
import com.sourceplusplus.mentor.impl.task.analyze.CalculateLinearRegression.Companion.REGRESSION_MAP
import com.sourceplusplus.mentor.impl.task.analyze.DetermineTraceSpanArtifact
import com.sourceplusplus.protocol.advice.AdviceType
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.advice.cautionary.RampDetectionAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class CreateArtifactAdvice(
    private val byArtifactAdviceContext: ContextKey<List<ArtifactAdvice>>? = null,
    private val byArtifactsContext: ContextKey<List<ArtifactQualifiedName>>? = null,
    adviceType: AdviceType
) : MentorTask() {

    override val inputContextKeys: List<ContextKey<*>> = listOfNotNull(
        byArtifactAdviceContext,
        byArtifactsContext
    )

    override suspend fun executeTask(job: MentorJob) {
        when {
            byArtifactAdviceContext != null -> {
                job.context.get(byArtifactAdviceContext).forEach {
                    job.addAdvice(it)
                }
            }
            byArtifactsContext != null -> {
                job.context.get(byArtifactsContext).forEach {
                    //todo: cannot assume DetermineTraceSpanArtifact.RESOLUTION_MAP exists
                    val endpointName = job.context.get(DetermineTraceSpanArtifact.RESOLUTION_MAP)[it]!!
                    val regression = job.context.get(REGRESSION_MAP)[endpointName]!! //todo: or this
                    job.addAdvice(
                        RampDetectionAdvice(
                            ArtifactQualifiedName(endpointName, "todo", ArtifactType.ENDPOINT),
                            it,
                            CalculateLinearRegression.ApacheSimpleRegression(regression)
                        ) //todo: switch on advice type
                    )
                }
            }
        }
    }
}
