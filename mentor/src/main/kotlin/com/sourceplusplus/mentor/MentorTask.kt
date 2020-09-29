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
        set(value) {
            field = value
            //todo: remove/add to queue
        }
    abstract val contextKeys: List<ContextKey<*>>

    fun withPriority(priority: Int): MentorTask {
        this.priority = priority
        return this
    }

    abstract suspend fun executeTask(job: MentorJob)
    override operator fun compareTo(other: MentorTask): Int = priority.compareTo(other.priority)
    override fun toString(): String = javaClass.simpleName
}