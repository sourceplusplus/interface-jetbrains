package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.SkywalkingMonitor
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class GetTracesTest {

    @Test(timeout = 2500)
    fun latestTraces() {
        val vertx = Vertx.vertx()
        vertx.eventBus().registerDefaultCodec(
            TraceResult::class.java,
            SkywalkingClient.LocalMessageCodec<TraceResult>()
        )
        val emptyJob = object : MentorJob() {
            override val vertx: Vertx = vertx
            override val tasks: List<MentorTask> = listOf()
        }

        runBlocking(vertx.dispatcher()) {
            vertx.deployVerticleAwait(SkywalkingMonitor("http://localhost:12800/graphql"))
            GetTraces(
                orderType = TraceOrderType.LATEST_TRACES,
                timeFrame = QueryTimeFrame.LAST_15_MINUTES
            ).executeTask(emptyJob)

            assertNotNull(emptyJob.context.get(GetTraces.TRACE_RESULT))
            vertx.close()
        }
    }
}