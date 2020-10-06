package com.sourceplusplus.mentor

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
    private val stillValidTasks = mutableListOf<MentorTask>()
    private var running = false
    private val adviceListeners: MutableList<AdviceListener> = mutableListOf()

    override suspend fun start() {
        log.info("Setting up SourceMentor")

        launch(vertx.dispatcher()) {
            runJobProcessing()
        }
    }

    private suspend fun runJobProcessing() {
        running = true
        while (running) {
            var currentTask: MentorTask = runInterruptible(Dispatchers.IO) { taskQueue.take() }

            //find jobs requiring task (execute once then share results)
            val jobsWhichRequireTask = jobList.filter { it.isCurrentTask(currentTask) }

            //search still valid tasks for current task
            var reusingTask = false
            val stillValidTask: MentorTask? = null //stillValidTasks.find { it == currentTask }
            if (stillValidTask != null) {
                currentTask = stillValidTask
                reusingTask = true
            } else {
                jobsWhichRequireTask[0].log("Executing task: $currentTask")
                currentTask.executeTask(jobsWhichRequireTask[0])
                if (!currentTask.asyncTask) {
                    jobsWhichRequireTask[0].log("Executed task: $currentTask")
                }

                if (currentTask.remainValidDuration > 0) {
                    stillValidTasks.add(currentTask)

                    launch(vertx.dispatcher()) {
                        delay(currentTask.remainValidDuration)
                        stillValidTasks.removeIf { it === currentTask }
                    }
                }
            }

            if (!reusingTask && currentTask.asyncTask) {
                currentTask.getAsyncFuture().onComplete {
                    jobsWhichRequireTask[0].log("Executed task: $currentTask")
                    handleTaskCompletion(jobsWhichRequireTask, currentTask, false)
                }
            } else {
                handleTaskCompletion(jobsWhichRequireTask, currentTask, reusingTask)
            }
        }
    }

    private fun handleTaskCompletion(
        jobsWhichRequireTask: List<MentorJob>,
        currentTask: MentorTask,
        reusingTask: Boolean
    ) {
        val tasksStillRequired = Collections.newSetFromMap(
            IdentityHashMap<MentorTask, Boolean>()
        )
        val startIndex = if (reusingTask) 0 else 1
        for (i in startIndex until jobsWhichRequireTask.size) {
//            if (reusingTask) {
//                println("here")
//            }
            val sameContext = jobsWhichRequireTask[i].currentTask()
                .usingSameContext(jobsWhichRequireTask[i], jobsWhichRequireTask[0], currentTask)
            if (sameContext) {
                jobsWhichRequireTask[i].context.copyContext(jobsWhichRequireTask[0], currentTask)
                jobsWhichRequireTask[i].log("Copied context for task: $currentTask")
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
                    it.log("Rescheduled job for: {}")
                    it.emitEvent(MentorJobEvent.JOB_RESCHEDULED)

                    //todo: reschedule job logic
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
        }
    }

    fun executeJob(job: MentorJob) {
        if (job.tasks.isEmpty()) {
            throw IllegalArgumentException("Job contains no tasks")
        }

        adviceListeners.forEach(job::addAdviceListener)
        jobList.add(job)
        addTask(job.nextTask())
    }

    fun executeJobs(vararg jobs: MentorJob) {
        jobs.forEach(this@SourceMentor::executeJob)
    }

    fun addAdviceListener(adviceListener: AdviceListener) {
        adviceListeners.add(adviceListener)
    }
}
