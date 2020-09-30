package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.protocol.artifact.ArtifactLocation
import com.sourceplusplus.protocol.artifact.trace.TraceResult

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class DetermineThrowableLocation(
    private val byTracesContext: ContextKey<TraceResult>
) : MentorTask() {

    companion object {
        val ARTIFACT_LOCATION: ContextKey<ArtifactLocation> = ContextKey()
    }

    override val contextKeys = listOf(ARTIFACT_LOCATION)

    override suspend fun executeTask(job: MentorJob) {
        job.log("Executing task: $this")

        TODO("Not yet implemented")
    }
}