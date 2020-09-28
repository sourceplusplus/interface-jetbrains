package com.sourceplusplus.mentor.task

import com.sourceplusplus.mentor.MentorJob
import com.sourceplusplus.mentor.MentorJob.ContextKey
import com.sourceplusplus.mentor.MentorTask
import com.sourceplusplus.protocol.artifact.ArtifactLocation

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class DetermineThrowableLocation(
    private val byTracesContext: ContextKey<Nothing>
) : MentorTask() {

    companion object {
        val ARTIFACT_LOCATION: ContextKey<ArtifactLocation> = ContextKey()
    }

    override val contextKeys = listOf(ARTIFACT_LOCATION)

    override suspend fun executeTask(job: MentorJob) {
        TODO("Not yet implemented")
    }
}