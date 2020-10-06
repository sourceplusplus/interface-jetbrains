package com.sourceplusplus.mentor.impl.job

import com.sourceplusplus.mentor.MentorTest
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.mentor.base.MentorJobEvent
import com.sourceplusplus.mentor.impl.task.monitor.GetService
import io.vertx.core.Promise
import io.vertx.kotlin.core.onCompleteAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class RampDetectionMentorTest : MentorTest() {

    @Test(timeout = 15_000)
    fun testJob() {
        val testPromise = Promise.promise<Nothing>()
        val job = RampDetectionMentor(vertx)

        val mentor = SourceMentor()
        mentor.executeJob(job)//.withConfig(MentorJobConfig(repeatForever = true)))
        job.addJobListener { event, _ ->
            if (event == MentorJobEvent.JOB_COMPLETE) {
                assertNotNull(job.context.get(GetService.SERVICE))
                testPromise.complete()
            }
        }

        runBlocking(vertx.dispatcher()) {
            vertx.deployVerticle(mentor)
            testPromise.future().onCompleteAwait()
            val stopPromise = Promise.promise<Void>()
            mentor.stop(stopPromise)
            stopPromise.future().onCompleteAwait()
        }
    }
}

