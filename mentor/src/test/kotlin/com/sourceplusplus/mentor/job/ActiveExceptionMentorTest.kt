package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.MentorJobEvent
import com.sourceplusplus.mentor.MentorTest
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.mentor.task.GetService
import io.vertx.core.Promise
import io.vertx.kotlin.core.onCompleteAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class ActiveExceptionMentorTest : MentorTest() {

    @Test(timeout = 2500)
    fun testJob() {
        val testPromise = Promise.promise<Nothing>()
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
