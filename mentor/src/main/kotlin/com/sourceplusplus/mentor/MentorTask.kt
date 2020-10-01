package com.sourceplusplus.mentor

import com.sourceplusplus.mentor.MentorJob.ContextKey

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class MentorTask : Comparable<MentorTask> {

    var priority: Int = 0
    abstract val contextKeys: List<ContextKey<*>>

    fun withPriority(priority: Int): MentorTask {
        this.priority = priority
        return this
    }

    abstract suspend fun executeTask(job: MentorJob)
    override operator fun compareTo(other: MentorTask): Int = other.priority.compareTo(priority)
    override fun toString(): String = javaClass.simpleName
    open fun usingSameContext(selfJob: MentorJob, job: MentorJob, task: MentorTask): Boolean = true
}