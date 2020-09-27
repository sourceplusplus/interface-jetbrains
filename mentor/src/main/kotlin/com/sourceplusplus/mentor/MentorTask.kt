package com.sourceplusplus.mentor

import java.util.*

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

    abstract suspend fun executeTask(job: MentorJob, context: TaskContext)

    override operator fun compareTo(other: MentorTask): Int = priority.compareTo(other.priority)

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
    }

    @Suppress("unused")
    class ContextKey<T>
}