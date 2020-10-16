package com.sourceplusplus.mentor.impl.task

import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.MentorTest
import com.sourceplusplus.mentor.impl.task.monitor.GetTraceStacks
import com.sourceplusplus.mentor.impl.task.monitor.GetTraces
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class GetTraceStacksTest : MentorTest() {

    @Test(timeout = 5000)
    fun latestTraceStacks() {
        val emptyJob = object : MentorJob() {
            override val vertx: Vertx = this@GetTraceStacksTest.vertx
            override val tasks: List<MentorTask> = listOf()
        }

        runBlocking(vertx.dispatcher()) {
            GetTraces(
                orderType = TraceOrderType.LATEST_TRACES,
                timeFrame = QueryTimeFrame.LAST_15_MINUTES
            ).executeTask(emptyJob)
            GetTraceStacks(
                GetTraces.TRACE_RESULT
            ).executeTask(emptyJob)

            assertNotNull(emptyJob.context.get(GetTraceStacks.TRACE_STACKS))
        }
    }
}