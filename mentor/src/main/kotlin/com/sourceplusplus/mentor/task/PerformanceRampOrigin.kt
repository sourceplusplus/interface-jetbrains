package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PerformanceRampOrigin : MentorTask() {

    companion object {
    }

    override val contextKeys = listOf<MentorJob.ContextKey<*>>()

    override suspend fun executeTask(job: MentorJob) {
        //todo: search for likely causes (SQL no limit, etc)
        TODO("Not yet implemented")
    }
}