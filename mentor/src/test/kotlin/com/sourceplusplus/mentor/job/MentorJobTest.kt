package com.sourceplusplus.mentor.job

import com.sourceplusplus.mentor.*
import com.sourceplusplus.mentor.task.GetService
import com.sourceplusplus.mentor.task.GetServiceInstance
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.core.onCompleteAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

class MentorJobTest : MentorTest() {

    @Test(timeout = 20_000)
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

//    @Test(timeout = 15000)
//    fun shouldNotShareContext() {
//        val simpleJob1 = object : MentorJob() {
//            override val vertx: Vertx = this@MentorJobTest.vertx
//            override val tasks: List<MentorTask> = listOf(
//                GetService(), GetServiceInstance(GetService.SERVICE)
//            )
//        }
//        val simpleJob2 = object : MentorJob() {
//            override val vertx: Vertx = this@MentorJobTest.vertx
//            override val tasks: List<MentorTask> = listOf(
//                GetService(byName = "Nothing"), GetServiceInstance(GetService.SERVICE)
//            )
//        }
//
//        val mentor = SourceMentor()
//        mentor.executeJobs(simpleJob1, simpleJob2)
//        vertx.deployVerticle(mentor)
//
//        val job1Promise = Promise.promise<Nothing>()
//        simpleJob1.addJobListener { event, _ ->
//            if (event == MentorJobEvent.JOB_COMPLETE) {
//                job1Promise.complete()
//            } else if (event == MentorJobEvent.CONTEXT_SHARED) {
//                fail("Shared context")
//            }
//        }
//        val job2Promise = Promise.promise<Nothing>()
//        simpleJob2.addJobListener { event, _ ->
//            if (event == MentorJobEvent.JOB_COMPLETE) {
//                job2Promise.complete()
//            } else if (event == MentorJobEvent.CONTEXT_SHARED) {
//                fail("Shared context")
//            }
//        }
//        runBlocking(vertx.dispatcher()) {
//            job1Promise.future().onCompleteAwait()
//            job2Promise.future().onCompleteAwait()
//            val stopPromise = Promise.promise<Void>()
//            mentor.stop(stopPromise)
//            stopPromise.future().onCompleteAwait()
//        }
//    }
}
