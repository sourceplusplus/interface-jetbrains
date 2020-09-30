package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.MentorJobEvent
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.mentor.task.GetService
import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.SkywalkingMonitor
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.onCompleteAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class ActiveExceptionMentorTest {

    @Test(timeout = 2500)
    fun testJob() {
        val testPromise = Promise.promise<Nothing>()
        val vertx = Vertx.vertx()
        vertx.eventBus().registerDefaultCodec(
            TraceResult::class.java,
            SkywalkingClient.LocalMessageCodec<TraceResult>()
        )
        vertx.eventBus().registerDefaultCodec(
            TraceSpanStackQueryResult::class.java,
            SkywalkingClient.LocalMessageCodec<TraceSpanStackQueryResult>()
        )
        runBlocking(vertx.dispatcher()) {
            vertx.deployVerticleAwait(SkywalkingMonitor("http://localhost:12800/graphql"))
        }
        val job = ActiveExceptionMentor(vertx, "spp.example")

        val mentor = SourceMentor()
        mentor.executeJob(job)
        vertx.deployVerticle(mentor)

        job.addJobListener { event, _ ->
            if (event == MentorJobEvent.JOB_COMPLETE) {
                assertNotNull(job.context.get(GetService.SERVICE))
                testPromise.complete()
            }
        }
        runBlocking(vertx.dispatcher()) {
            testPromise.future().onCompleteAwait()
            vertx.close()
        }
    }
}
