package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.mentor.MentorTest
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateLinearRegressionTest : MentorTest() {

    @Test(timeout = 30_000)
    fun calculateSlope() {
        val emptyJob = object : MentorJob() {
            override val vertx: Vertx = this@CalculateLinearRegressionTest.vertx
            override val tasks: List<MentorTask> = listOf()
        }

        runBlocking(vertx.dispatcher()) {
            delay(5000)

            val getTraces = GetTraces(
                orderType = TraceOrderType.LATEST_TRACES,
                timeFrame = QueryTimeFrame.LAST_15_MINUTES,
                endpointName = "{GET}/users",
                limit = 100
            )
            getTraces.executeTask(emptyJob)
            val calcRegression =
                CalculateLinearRegression(
                    GetTraces.TRACE_RESULT
                )
            calcRegression.executeTask(emptyJob)

            delay(5000)

            getTraces.executeTask(emptyJob)
            calcRegression.executeTask(emptyJob)

            delay(5000)

            getTraces.executeTask(emptyJob)
            calcRegression.executeTask(emptyJob)

            assertNotNull(calcRegression.regressionMap["{GET}/users"])
            assertNotNull(calcRegression.regressionMap["{GET}/users/{id}"])
            assertTrue(
                calcRegression.regressionMap["{GET}/users"]!!.slope >
                        calcRegression.regressionMap["{GET}/users/{id}"]!!.slope
            )
        }
    }
}