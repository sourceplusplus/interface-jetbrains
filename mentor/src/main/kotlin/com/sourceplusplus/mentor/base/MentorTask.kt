package com.sourceplusplus.mentor.base

import io.vertx.core.Future

/**
 * Base class for sharable and reusable tasks.
 *
 * Must implement [equals]/[hashCode]/[inputContextKeys] to be considered for task sharing.
 *
 * Must implement [remainValidDuration] for consideration for task reuse.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class MentorTask(
    open val asyncTask: Boolean = false
) : Comparable<MentorTask> {

    open val remainValidDuration: Long = 0
    var priority: Int = 0
    open val inputContextKeys: List<ContextKey<*>> = listOf()
    open val outputContextKeys: List<ContextKey<*>> = listOf()

    fun withPriority(priority: Int): MentorTask {
        this.priority = priority
        return this
    }

    abstract suspend fun executeTask(job: MentorJob)
    override operator fun compareTo(other: MentorTask): Int = other.priority.compareTo(priority)
    override fun toString(): String = "${javaClass.simpleName}@${System.identityHashCode(this)}"

    fun usingSameContext(selfJob: MentorJob, otherContext: MentorTaskContext, task: MentorTask): Boolean {
        return usingSameContext(selfJob.context, otherContext, task)
    }

    open fun usingSameContext(
        selfContext: MentorTaskContext,
        otherContext: MentorTaskContext,
        task: MentorTask
    ): Boolean {
        return if (task.inputContextKeys.isEmpty()) {
            false
        } else {
            task.inputContextKeys.all { selfContext.get(it) == otherContext.get(it) }
        }
    }

    open fun getAsyncFuture(): Future<Nothing> {
        throw UnsupportedOperationException("Must override method to implement")
    }
}
