package com.sourceplusplus.mentor.task.general

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

    //todo: shouldn't work by delay but by rescheduling; otherwise delays concurrent jobs
    override suspend fun executeTask(job: MentorJob) {
        delay(delay)
    }
}