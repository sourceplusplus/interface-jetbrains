package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import kotlinx.coroutines.delay

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class DelayTask(private val delay: Long) : MentorTask() {

    override suspend fun executeTask(job: MentorJob) {
        delay(delay)
    }
}