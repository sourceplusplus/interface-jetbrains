package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.protocol.artifact.trace.TraceResult

class CalculateLinearRegression(
    private val byTracesContext: MentorJob.ContextKey<TraceResult>,
    private val rootPackage: String
) : MentorTask() {

    companion object {
    }

    override val contextKeys = listOf<MentorJob.ContextKey<*>>()

    override suspend fun executeTask(job: MentorJob) {
        TODO("Not yet implemented")
    }
}