package com.sourceplusplus.mentor.impl.task.analyze

import com.sourceplusplus.mentor.base.ContextKey
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.base.MentorTaskContext
import com.sourceplusplus.protocol.advice.cautionary.RampDetectionAdvice
import com.sourceplusplus.protocol.artifact.trace.Trace
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import org.apache.commons.math3.stat.regression.SimpleRegression

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class CalculateLinearRegression(
    private val byTracesContext: ContextKey<TraceResult>,
    private val confidence: Double,
    private val minimumSampleCount: Int = 100,
    val regressionMap: MutableMap<String, SimpleRegression> = mutableMapOf()
) : MentorTask() {

    companion object {
        val TRACES: ContextKey<List<Trace>> = ContextKey("CalculateLinearRegression.TRACES")
        val REGRESSION_MAP: ContextKey<Map<String, SimpleRegression>> =
            ContextKey("CalculateLinearRegression.REGRESSION_MAP")
    }

    override suspend fun executeTask(job: MentorJob) {
        job.trace("Task configuration\n\tbyTracesContext: $byTracesContext")

        val traceResult = job.context.get(byTracesContext)
        for (trace in traceResult.traces) {
            regressionMap.putIfAbsent(trace.operationNames[0], SimpleRegression())
            val regression = regressionMap[trace.operationNames[0]]!!
            regression.addData(trace.start.toEpochMilliseconds().toDouble(), trace.duration.toDouble())
        }

        //todo: there should likely be a way to give endpoints priority
        // based on the likelihood for it to be a performance ramp

        val offendingTraces = mutableListOf<Trace>()
        regressionMap.forEach { entry ->
            if (entry.value.slope >= 0 && entry.value.rSquare >= confidence && entry.value.n >= minimumSampleCount) {
                offendingTraces.addAll(traceResult.traces.filter { it.operationNames[0] == entry.key })
            }
        }

        if (offendingTraces.isNotEmpty()) {
            job.log("Found ${offendingTraces.size} offending traces")
        }
        job.context.put(TRACES, offendingTraces)
        job.context.put(REGRESSION_MAP, regressionMap)
    }

    /**
     * This task uses a persistent regression map so two tasks should never assume their using the same context.
     */
    override fun usingSameContext(
        selfContext: MentorTaskContext,
        otherContext: MentorTaskContext,
        task: MentorTask
    ): Boolean = false

    /**
     * todo: description.
     */
    class ApacheSimpleRegression(private val sr: SimpleRegression) : RampDetectionAdvice.SimpleRegression {
        override val n get() = sr.n
        override val intercept get() = sr.intercept
        override val slope get() = sr.slope
        override val sumSquaredErrors get() = sr.sumSquaredErrors
        override val totalSumSquares get() = sr.totalSumSquares
        override val xSumSquares get() = sr.xSumSquares
        override val sumOfCrossProducts get() = sr.sumOfCrossProducts
        override val regressionSumSquares get() = sr.regressionSumSquares
        override val meanSquareError get() = sr.meanSquareError
        override val r get() = sr.r
        override val rSquare get() = sr.rSquare
        override val interceptStdErr get() = sr.interceptStdErr
        override val slopeStdErr get() = sr.slopeStdErr
        override val slopeConfidenceInterval get() = sr.slopeConfidenceInterval
        override val significance get() = sr.significance
        override fun predict(x: Double) = sr.predict(x)
    }
}
