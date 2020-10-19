package com.sourceplusplus.mentor.base

import java.util.*

/**
 * Used as a persistent context between [MentorTask]s when processing a [MentorJob].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class MentorTaskContext {

    private val cache: IdentityHashMap<ContextKey<*>, Any> = IdentityHashMap()

    fun <T> put(key: ContextKey<T>, value: T) {
        cache[key] = value!!
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: ContextKey<T>): T {
        return cache[key] as T
    }

    internal fun clear() {
        cache.clear()
    }

    fun copyOutputContext(copyJob: MentorJob, copyTask: MentorTask) {
        copyOutputContext(copyJob.context, copyTask)
    }

    fun copyOutputContext(copyContext: MentorTaskContext, copyTask: MentorTask) {
        copyTask.outputContextKeys.forEach {
            cache[it] = copyContext.get(it)
        }
    }

    fun copyFullContext(copyJob: MentorJob, copyTask: MentorTask) {
        copyFullContext(copyJob.context, copyTask)
    }

    fun copyFullContext(copyContext: MentorTaskContext, copyTask: MentorTask) {
        copyTask.inputContextKeys.forEach {
            cache[it] = copyContext.get(it)
        }
        copyTask.outputContextKeys.forEach {
            cache[it] = copyContext.get(it)
        }
    }
}
