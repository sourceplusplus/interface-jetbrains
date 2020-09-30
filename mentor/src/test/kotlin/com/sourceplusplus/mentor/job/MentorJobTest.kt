package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJobEvent
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.mentor.task.GetService
import com.sourceplusplus.monitor.skywalking.SkywalkingMonitor
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.onCompleteAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class MentorJobTest {

    @Test(timeout = 2500)
    fun singleTaskJob() {
        val testPromise = Promise.promise<Nothing>()
        val vertx = Vertx.vertx()
        runBlocking(vertx.dispatcher()) {
            vertx.deployVerticleAwait(SkywalkingMonitor("http://localhost:12800/graphql"))
        }
        val simpleJob = object : MentorJob() {
            override val vertx: Vertx = vertx
            override val tasks: List<MentorTask> = listOf(GetService())
        }

        val mentor = SourceMentor()
        mentor.executeJob(simpleJob)
        vertx.deployVerticle(mentor)

        simpleJob.addJobListener { event, _ ->
            if (event == MentorJobEvent.JOB_COMPLETE) {
                assertNotNull(simpleJob.context.get(GetService.SERVICE))
                testPromise.complete()
            }
        }
        runBlocking(vertx.dispatcher()) {
            testPromise.future().onCompleteAwait()
            vertx.close()
        }
    }
}
