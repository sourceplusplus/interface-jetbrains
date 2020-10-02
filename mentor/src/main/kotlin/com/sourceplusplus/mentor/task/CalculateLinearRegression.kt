package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import org.apache.commons.math3.stat.regression.SimpleRegression

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class CalculateLinearRegression(
    private val byTracesContext: ContextKey<TraceResult>,
    val regressionMap: MutableMap<String, SimpleRegression> = mutableMapOf()
) : MentorTask() {

    companion object {
    }

    override val contextKeys = listOf<ContextKey<*>>()

    override suspend fun executeTask(job: MentorJob) {
        job.log("Executing task: $this")
        job.log("Task configuration\n\tbyTracesContext: $byTracesContext")

        val traceResult = job.context.get(byTracesContext)
        for (trace in traceResult.traces) {
            regressionMap.putIfAbsent(trace.operationNames[0], SimpleRegression())
            val regression = regressionMap[trace.operationNames[0]]!!
            regression.addData(trace.start.toDouble(), trace.duration.toDouble())
        }

        //todo: should be saving regression objects permanently to job
        //todo: each time run should add new traces to regression object
        //todo: there should likely be a way to give endpoints priority based on the likelihood for it to be a performance ramp

        regressionMap.forEach {
            if (it.value.slope >= 0 && it.value.rSquare >= 0.50 && it.value.n >= 100) {
                println("apparent linear slope detected")
            }
//            println(
//                "${it.key}\n\t" +
//                        "slope: ${it.value.slope}\n\t" +
//                        "slopeConfidenceInterval: ${it.value.slopeConfidenceInterval}\n\t" +
//                        "r: ${it.value.r}\n\t" +
//                        "rSquare: ${it.value.rSquare}\n\t" +
//                        "slopeStdErr: ${it.value.slopeStdErr}\n\t" +
//                        "intercept: ${it.value.intercept}\n\t" +
//                        "interceptStdErr: ${it.value.interceptStdErr}\n\t" +
//                        "significance: ${it.value.significance}\n\t" +
//                        "n: ${it.value.n}"
//            )
        }
    }
}