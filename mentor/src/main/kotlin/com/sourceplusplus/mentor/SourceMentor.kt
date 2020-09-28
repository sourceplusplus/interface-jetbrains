package com.sourceplusplus.mentor

import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.PriorityBlockingQueue

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMentor : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(SourceMentor::class.java)
    }

    private val jobList = mutableListOf<MentorJob>()
    private val taskQueue = PriorityBlockingQueue<MentorTask>()
    private var running = false

    override suspend fun start() {
        log.info("Setting up SourceMentor")

        launch {
            runJobProcessing()
        }
    }

    private suspend fun runJobProcessing() {
        running = true
        while (running) {
            val currentTask = taskQueue.poll()
            //todo: search un-expired tasks before executing current task

            //find jobs requiring task (execute once then share results)
            val jobsWhichRequireTask = jobList.filter { it.isCurrentTask(currentTask) }
            currentTask.executeTask(jobsWhichRequireTask[0])
            for (i in 1 until jobsWhichRequireTask.size) {
                jobsWhichRequireTask[i].context.copyContext(jobsWhichRequireTask[0], currentTask)
            }

            //add new tasks to queue
            jobsWhichRequireTask.forEach {
                if (it.hasMoreTasks()) {
                    val nextTask = it.nextTask()
                    if (!taskQueue.contains(nextTask)) {
                        taskQueue.add(nextTask)
                    }
                }
            }

            //todo: task context expiration list
        }
    }

    fun executeJob(job: MentorJob) {
        if (job.tasks.isEmpty()) {
            throw IllegalArgumentException("Job contains no tasks")
        }

        jobList.add(job)
        val task = job.nextTask()
        if (!taskQueue.contains(task)) {
            taskQueue.add(task)
        }
    }

    fun executeJobs(vararg jobs: MentorJob) {
        jobs.forEach(this@SourceMentor::executeJob)
    }

    fun getAllMethodAdvice(methodQualifiedName: ArtifactQualifiedName): List<ArtifactAdvice> {
        return emptyList() //todo: this
    }
}
