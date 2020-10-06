package com.sourceplusplus.mentor.base

import com.sourceplusplus.protocol.advice.AdviceListener
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

/**
 * Keeps track of the order and current position of [MentorTask]s which require processing.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class MentorJob {

    companion object {
        private val log = LoggerFactory.getLogger(MentorJob::class.java)
    }

    abstract val vertx: Vertx
    var config: MentorJobConfig = MentorJobConfig()
        private set
    abstract val tasks: List<MentorTask>
    val context = MentorTaskContext()
    private var currentTask = -1
    private var complete: Boolean = false
    private val listeners: MutableList<MentorJobListener> = mutableListOf()
    private val adviceListeners: MutableList<AdviceListener> = mutableListOf()

    fun addJobListener(jobListener: MentorJobListener) = listeners.add(jobListener)
    fun currentTask(): MentorTask = tasks[currentTask]
    fun nextTask(): MentorTask = tasks[++currentTask]
    fun hasMoreTasks(): Boolean = !complete && currentTask < (tasks.size - 1)
    fun isCurrentTask(task: MentorTask): Boolean = !complete && currentTask > -1 && tasks[currentTask] == task

    fun complete() {
        if (complete) {
            throw IllegalStateException("Job already complete")
        }
        complete = true
        log("Job completed")
        emitEvent(MentorJobEvent.JOB_COMPLETE)
    }

    fun log(msg: String) {
        log.debug("{$this}\n$msg\n")
    }

    fun resetJob() {
        log("Job reset")
        currentTask = -1
        context.clear()
    }

    fun withConfig(config: MentorJobConfig): MentorJob {
        this.config = config
        return this
    }

    fun emitEvent(event: MentorJobEvent, data: Any? = null) {
        listeners.forEach { it.onEvent(event, data) }
    }

    suspend fun addAdvice(artifactAdvice: ArtifactAdvice) {
        adviceListeners.forEach { it.advised(artifactAdvice) }
    }

    fun addAdviceListener(adviceListener: AdviceListener) {
        adviceListeners.add(adviceListener)
    }

    override fun toString(): String = "${javaClass.simpleName}@${System.identityHashCode(this)}"
}