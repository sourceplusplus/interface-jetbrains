package com.sourceplusplus.protocol.advice.informative

import com.sourceplusplus.protocol.advice.AdviceCategory
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActiveExceptionAdvice(
    artifact: ArtifactQualifiedName,
    stackTrace: JvmStackTrace
) : ArtifactAdvice(artifact, AdviceCategory.INFORMATIVE) {

    var stackTrace: JvmStackTrace = stackTrace
        private set

    fun updateStackTrace(stackTrace: JvmStackTrace) {
        this.stackTrace = stackTrace
        triggerUpdated()
    }

    /**
     * Compares everything except [JvmStackTrace.message] as the message may just contain timestamp differences
     * even though everything else is equivalent.
     */
    override fun isSameArtifactAdvice(artifactAdvice: ArtifactAdvice): Boolean {
        return artifactAdvice is ActiveExceptionAdvice && artifactAdvice.artifact == artifact &&
                artifactAdvice.stackTrace.exceptionType == stackTrace.exceptionType &&
                artifactAdvice.stackTrace.elements == stackTrace.elements &&
                artifactAdvice.stackTrace.causedBy == stackTrace.causedBy
    }

    override fun updateArtifactAdvice(artifactAdvice: ArtifactAdvice) {
        updateStackTrace((artifactAdvice as ActiveExceptionAdvice).stackTrace)
    }
}