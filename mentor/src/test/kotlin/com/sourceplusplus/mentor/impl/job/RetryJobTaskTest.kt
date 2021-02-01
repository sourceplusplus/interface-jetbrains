package com.sourceplusplus.mentor.impl.job

import com.sourceplusplus.mentor.base.MentorJobEvent
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorJobConfig
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.impl.task.monitor.GetService
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RetryJobTaskTest {

    val vertx: Vertx = Vertx.vertx()

    @Test(timeout = 5000)
    fun retryJob() {
        val testPromise = Promise.promise<Nothing>()
        val job = object : MentorJob() {
            override val vertx: Vertx = this@RetryJobTaskTest.vertx
            override val tasks: List<MentorTask> = listOf(GetService())
        }.withConfig(MentorJobConfig(repeatForever = true))

        var failCount = 0
        val mentor = SourceMentor()
        mentor.addJob(job)
        job.addJobListener { event, _ ->
            if (event == MentorJobEvent.TASK_FAILED) {
                failCount++
                if (failCount > 1) {
                    testPromise.complete()
                }
            }
        }

        runBlocking(vertx.dispatcher()) {
            vertx.deployVerticle(mentor)
            testPromise.future().await()
            val stopPromise = Promise.promise<Void>()
            mentor.stop(stopPromise)
            stopPromise.future().await()
        }
    }
}
