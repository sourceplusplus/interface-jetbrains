package com.sourceplusplus.mentor.base

import com.sourceplusplus.mentor.base.MentorJob.TaskContext
import com.sourceplusplus.protocol.advice.AdviceListener
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Primarily used to propagate a persistent context ([TaskContext]) between [MentorTask]s.
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
    val context = TaskContext()
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

    class TaskContext {
        private val cache: IdentityHashMap<ContextKey<*>, Any> = IdentityHashMap()

        fun <T> put(key: ContextKey<T>, value: T) {
            cache[key] = value!!
        }

        fun <T> get(key: ContextKey<T>): T {
            return cache[key] as T
        }

        internal fun clear() {
            cache.clear()
        }

        fun copyOutputContext(copyJob: MentorJob, copyTask: MentorTask) {
            copyOutputContext(copyJob.context, copyTask)
        }

        fun copyOutputContext(copyContext: TaskContext, copyTask: MentorTask) {
            copyTask.outputContextKeys.forEach {
                cache[it] = copyContext.get(it)
            }
        }

        fun copyFullContext(copyJob: MentorJob, copyTask: MentorTask) {
            copyFullContext(copyJob.context, copyTask)
        }

        fun copyFullContext(copyContext: TaskContext, copyTask: MentorTask) {
            copyTask.inputContextKeys.forEach {
                cache[it] = copyContext.get(it)
            }
            copyTask.outputContextKeys.forEach {
                cache[it] = copyContext.get(it)
            }
        }
    }

    @Suppress("unused")
    class ContextKey<T>(private val name: String) {
        override fun toString(): String = name
    }
}