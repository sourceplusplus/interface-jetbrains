package com.sourceplusplus.mentor

import io.vertx.core.Vertx
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class MentorJob {

    abstract val vertx: Vertx
    var config: MentorJobConfig = MentorJobConfig()
        private set
    abstract val tasks: List<MentorTask>
    val context = TaskContext()
    private var currentTask = -1
    private var complete: Boolean = false

    fun nextTask(): MentorTask = tasks[++currentTask]
    fun hasMoreTasks(): Boolean = !complete && currentTask < (tasks.size - 1)
    fun isCurrentTask(task: MentorTask): Boolean = !complete && currentTask > -1 && tasks[currentTask] == task
    fun isComplete(): Boolean = complete || !hasMoreTasks()
    fun complete() {
        if (complete) {
            throw IllegalStateException("Job already complete")
        }
        complete = true
        log("Job completed")
    }

    fun log(msg: String) {
        println(msg)
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

    class TaskContext {
        private val cache: IdentityHashMap<ContextKey<*>, Any> = IdentityHashMap()

        fun <T> put(key: ContextKey<T>, value: T) {
            cache[key] = value!!
        }

        fun <T> get(key: ContextKey<T>): T {
            return cache[key] as T
        }

        fun containsKey(key: ContextKey<*>): Boolean {
            return cache.containsKey(key)
        }

        internal fun clear() {
            cache.clear()
        }

        fun copyContext(copyJob: MentorJob, copyTask: MentorTask) {
            copyTask.contextKeys.forEach {
                cache[it] = copyJob.context.get(it)
            }
        }
    }

    @Suppress("unused")
    class ContextKey<T>
}