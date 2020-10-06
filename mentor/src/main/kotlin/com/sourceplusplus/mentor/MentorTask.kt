package com.sourceplusplus.mentor

import com.sourceplusplus.mentor.MentorJob.ContextKey
import io.vertx.core.Future

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class MentorTask(
    open val asyncTask: Boolean = false
) : Comparable<MentorTask> {

    open val remainValidDuration: Long = 0
    var priority: Int = 0
    open val contextKeys: List<ContextKey<*>> = listOf()

    fun withPriority(priority: Int): MentorTask {
        this.priority = priority
        return this
    }

    abstract suspend fun executeTask(job: MentorJob)
    override operator fun compareTo(other: MentorTask): Int = other.priority.compareTo(priority)
    override fun toString(): String = javaClass.simpleName
    open fun usingSameContext(selfJob: MentorJob, job: MentorJob, task: MentorTask): Boolean = false

    open fun getAsyncFuture(): Future<Nothing> {
        throw UnsupportedOperationException("Must override method to implement")
    }
}