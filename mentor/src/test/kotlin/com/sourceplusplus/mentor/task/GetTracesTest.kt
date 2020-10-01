package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.mentor.MentorTest
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class GetTracesTest : MentorTest() {

    @Test(timeout = 2500)
    fun latestTraces() {
        val emptyJob = object : MentorJob() {
            override val vertx: Vertx = this@GetTracesTest.vertx
            override val tasks: List<MentorTask> = listOf()
        }

        runBlocking(vertx.dispatcher()) {
            GetTraces(
                orderType = TraceOrderType.LATEST_TRACES,
                timeFrame = QueryTimeFrame.LAST_15_MINUTES
            ).executeTask(emptyJob)

            assertNotNull(emptyJob.context.get(GetTraces.TRACE_RESULT))
            vertx.close()
        }
    }
}