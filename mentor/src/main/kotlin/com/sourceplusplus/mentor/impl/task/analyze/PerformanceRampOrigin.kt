package com.sourceplusplus.mentor.impl.task.analyze

import com.sourceplusplus.mentor.base.MentorJob
import com.sourceplusplus.mentor.base.MentorTask

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PerformanceRampOrigin : MentorTask() {

    override suspend fun executeTask(job: MentorJob) {
        //todo: search for likely causes (SQL no limit, etc)
        TODO("Not yet implemented")
    }
}