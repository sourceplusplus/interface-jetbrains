package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask

class GetEndpoints(
    private val backoffConfig: BackoffConfig? = null
) : MentorTask() {

    companion object {
    }

    override val contextKeys = listOf<ContextKey<*>>()

    data class BackoffConfig(
        val config: HashMap<String, Int> = HashMap()
    )

    override suspend fun executeTask(job: MentorJob) {
        TODO("Not yet implemented")
    }
}