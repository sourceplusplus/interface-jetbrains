package com.sourceplusplus.mentor.impl.task

import com.sourceplusplus.mentor.MentorTest
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.impl.task.analyze.CalculateLinearRegression
import com.sourceplusplus.mentor.impl.task.monitor.GetService
import com.sourceplusplus.mentor.impl.task.monitor.GetTraces
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CalculateLinearRegressionTest : MentorTest() {

    @Test(timeout = 60_000)
    fun calculateSlope() {
        val emptyJob = object : MentorJob() {
            override val vertx: Vertx = this@CalculateLinearRegressionTest.vertx
            override val tasks: List<MentorTask> = listOf()
        }

        runBlocking(vertx.dispatcher()) {
            delay(5000)
            GetService().executeTask(emptyJob)
            val getTraces = GetTraces(
                GetService.SERVICE,
                orderType = TraceOrderType.LATEST_TRACES,
                timeFrame = QueryTimeFrame.LAST_15_MINUTES,
                endpointName = "{GET}/users",
                limit = 100
            )
            getTraces.executeTask(emptyJob)
            val calcRegression =
                CalculateLinearRegression(
                    GetTraces.TRACE_RESULT, 0.5
                )
            calcRegression.executeTask(emptyJob)

            delay(15000)

            getTraces.executeTask(emptyJob)
            calcRegression.executeTask(emptyJob)

            delay(15000)

            getTraces.executeTask(emptyJob)
            calcRegression.executeTask(emptyJob)

            assertNotNull(calcRegression.regressionMap["{GET}/users"])
            assertNotNull(calcRegression.regressionMap["{GET}/users/{id}"])
            assertNotEquals(Double.NaN, calcRegression.regressionMap["{GET}/users"]!!.slope)
            assertNotEquals(Double.NaN, calcRegression.regressionMap["{GET}/users/{id}"]!!.slope)
        }
    }
}