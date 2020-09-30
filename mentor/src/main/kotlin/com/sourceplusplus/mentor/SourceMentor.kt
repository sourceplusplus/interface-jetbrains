package com.sourceplusplus.mentor

import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
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

        launch(vertx.dispatcher()) {
            runJobProcessing()
        }
    }

    private suspend fun runJobProcessing() {
        running = true
        while (running) {
            val currentTask: MentorTask = runInterruptible(Dispatchers.IO) { taskQueue.take() }
            //todo: search un-expired tasks before executing current task

            //find jobs requiring task (execute once then share results)
            val jobsWhichRequireTask = jobList.filter { it.isCurrentTask(currentTask) }
            currentTask.executeTask(jobsWhichRequireTask[0])
            jobsWhichRequireTask[0].log("Executed task: $currentTask")

            for (i in 1 until jobsWhichRequireTask.size) {
                jobsWhichRequireTask[i].context.copyContext(jobsWhichRequireTask[0], currentTask)
                jobsWhichRequireTask[i].log("Copied context for task: $currentTask")
            }

            jobsWhichRequireTask.forEach {
                it.emitEvent(MentorJobEvent.TASK_COMPLETE, currentTask)

                if (it.isComplete()) {
                    //reschedule complete jobs (if necessary)
                    if (it.config.repeatForever) {
                        it.resetJob()
                        //todo: reschedule job logic
                        it.log("Rescheduled job for: {}")
                        it.emitEvent(MentorJobEvent.JOB_RESCHEDULED)
                    } else {
                        jobList.remove(it)
                        //it.emitEvent(MentorJobEvent.JOB_COMPLETE)
                    }
                } else if (it.hasMoreTasks()) {
                    //add new tasks to queue
                    //increment priority so completing jobs is considered more important than starting them
                    val nextTask = it.nextTask().withPriority(currentTask.priority + 1)
                    if (!taskQueue.contains(nextTask)) {
                        taskQueue.add(nextTask)
                    }
                } else {
                    it.complete()
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
