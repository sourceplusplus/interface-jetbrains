package com.sourceplusplus.mentor

import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorJobEvent
import com.sourceplusplus.mentor.base.MentorTask
import com.sourceplusplus.mentor.base.MentorTaskContext
import com.sourceplusplus.protocol.advice.AdviceListener
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.PriorityBlockingQueue

/**
 * Handles the processing of [MentorJob]s.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMentor : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(SourceMentor::class.java)
    }

    private val jobList = mutableListOf<MentorJob>()
    private val taskQueue = PriorityBlockingQueue<MentorTask>()
    private val stillValidTasks = mutableListOf<CachedMentorTask>()
    private var running = false
    private val adviceListeners: MutableList<AdviceListener> = mutableListOf()

    override suspend fun start() {
        log.info("Setting up SourceMentor")

        launch(vertx.dispatcher()) {
            try {
                runJobProcessing()
            } catch (throwable: Throwable) {
                log.error("Encountered fatal error processing jobs", throwable)
            }
        }
    }

    override suspend fun stop() {
        running = false
        stillValidTasks.clear()
        taskQueue.clear()
        jobList.clear()
        adviceListeners.clear()
        log.info("SourceMentor stopped")
    }

    private suspend fun runJobProcessing() {
        running = true
        while (running) {
            log.trace("Waiting for next task...")
            var currentTask: MentorTask = runInterruptible(Dispatchers.IO) { taskQueue.take() }
            if (!running) return
            log.trace("Processing task: $currentTask")

            //find jobs requiring task (execute once then share results)
            val jobsWhichRequireTask = jobList.filter { it.isCurrentTask(currentTask) }
            if (jobsWhichRequireTask.isEmpty()) continue

            //search still valid tasks for current task
            var reusingTask = false
            val stillValidTask = stillValidTasks.find { it.task == currentTask }
            if (stillValidTask != null) {
                log.trace("Found cached task: ${stillValidTask.task}")
                if (currentTask.usingSameContext(jobsWhichRequireTask[0], stillValidTask.context, currentTask)) {
                    currentTask = stillValidTask.task
                    reusingTask = true
                    jobsWhichRequireTask[0].context.copyOutputContext(stillValidTask.context, currentTask)
                    jobsWhichRequireTask[0].trace("Copied cached context for task: $currentTask")
                    jobsWhichRequireTask[0].emitEvent(MentorJobEvent.CONTEXT_REUSED, currentTask)
                } else {
                    executeTask(jobsWhichRequireTask[0], currentTask)
                }
            } else {
                executeTask(jobsWhichRequireTask[0], currentTask)
            }

            if (!reusingTask && currentTask.asyncTask) {
                currentTask.getAsyncFuture().onComplete {
                    jobsWhichRequireTask[0].trace("Executed task: $currentTask")
                    handleTaskCompletion(jobsWhichRequireTask, currentTask)
                }
            } else {
                handleTaskCompletion(jobsWhichRequireTask, currentTask)
            }
        }
    }

    private suspend fun executeTask(job: MentorJob, task: MentorTask) {
        if (!running) return
        job.trace("Executing task: $task")
        task.executeTask(job)
        if (!task.asyncTask) {
            job.trace("Executed task: $task")
        }

        if (task.remainValidDuration > 0) {
            job.trace("Caching task: $task")
            val cacheContext = MentorTaskContext()
            cacheContext.copyFullContext(job, task)

            stillValidTasks.add(CachedMentorTask(task, cacheContext))
            launch(vertx.dispatcher()) {
                delay(task.remainValidDuration)
                stillValidTasks.removeIf { it.task === task }
                job.trace("Removed cached task: $task")
            }
        }
    }

    private fun handleTaskCompletion(
        jobsWhichRequireTask: List<MentorJob>,
        currentTask: MentorTask
    ) {
        if (!running) return
        val tasksStillRequired = Collections.newSetFromMap(
            IdentityHashMap<MentorTask, Boolean>()
        )
        for (i in 1 until jobsWhichRequireTask.size) {
            val sameContext = jobsWhichRequireTask[i].currentTask()
                .usingSameContext(jobsWhichRequireTask[i].context, jobsWhichRequireTask[0].context, currentTask)
            if (sameContext) {
                jobsWhichRequireTask[i].context.copyOutputContext(jobsWhichRequireTask[0], currentTask)
                jobsWhichRequireTask[i].trace("Copied context for task: $currentTask")
                jobsWhichRequireTask[i].emitEvent(MentorJobEvent.CONTEXT_SHARED, currentTask)
            } else {
                val task = jobsWhichRequireTask[i].currentTask()
                tasksStillRequired.add(task)
                addTask(task)
            }
        }

        jobsWhichRequireTask.filter { !tasksStillRequired.contains(it.currentTask()) }.forEach {
            it.emitEvent(MentorJobEvent.TASK_COMPLETE, currentTask)

            if (it.hasMoreTasks()) {
                //add new tasks to queue
                addTask(it.nextTask())
            } else {
                //reschedule complete jobs (if necessary)
                if (it.config.repeatForever) {
                    it.resetJob()
                    it.emitEvent(MentorJobEvent.JOB_RESCHEDULED)
                    addTask(it.nextTask())
                } else {
                    it.complete()
                    jobList.remove(it)
                }
            }
        }
    }

    private fun addTask(task: MentorTask) {
        if (!taskQueue.contains(task)) {
            taskQueue.add(task)
            log.trace("Added task: $task")
        } else {
            log.trace("Ignoring duplicate task: $task")
        }
    }

    fun addJob(job: MentorJob) {
        if (job.tasks.isEmpty()) {
            throw IllegalArgumentException("Job contains no tasks")
        }

        adviceListeners.forEach(job::addAdviceListener)
        jobList.add(job)
        addTask(job.nextTask())
    }

    fun addJobs(vararg jobs: MentorJob) {
        jobs.forEach(this@SourceMentor::addJob)
    }

    fun addAdviceListener(adviceListener: AdviceListener) {
        adviceListeners.add(adviceListener)
    }

    private data class CachedMentorTask(
        val task: MentorTask,
        val context: MentorTaskContext
    )
}
