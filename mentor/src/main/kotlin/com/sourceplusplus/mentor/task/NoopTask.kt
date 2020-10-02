package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask

/**
 * No operation task.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class NoopTask : MentorTask() {
    override suspend fun executeTask(job: MentorJob) {
    }
}