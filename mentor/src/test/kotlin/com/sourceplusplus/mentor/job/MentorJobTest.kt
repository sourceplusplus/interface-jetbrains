package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.*
import com.sourceplusplus.mentor.task.GetService
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.core.onCompleteAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class MentorJobTest : MentorTest() {

    @Test(timeout = 5000)
    fun singleTaskJob() {
        val testPromise = Promise.promise<Nothing>()
        val simpleJob = object : MentorJob() {
            override val vertx: Vertx = this@MentorJobTest.vertx
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
            val stopPromise = Promise.promise<Void>()
            mentor.stop(stopPromise)
            stopPromise.future().onCompleteAwait()
        }
    }
}
