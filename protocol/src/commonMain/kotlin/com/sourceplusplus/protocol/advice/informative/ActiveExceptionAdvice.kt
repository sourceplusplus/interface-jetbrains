package com.sourceplusplus.protocol.advice.informative

import com.sourceplusplus.protocol.advice.AdviceCategory
import com.sourceplusplus.protocol.advice.AdviceType
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.exception.JvmStackTrace
import kotlinx.datetime.Instant

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ActiveExceptionAdvice(
    artifact: ArtifactQualifiedName,
    stackTrace: JvmStackTrace,
    occurredAt: Instant
) : ArtifactAdvice(artifact, AdviceCategory.INFORMATIVE, AdviceType.ActiveExceptionAdvice) {

    var stackTrace: JvmStackTrace = stackTrace
        private set
    var occurredAt: Instant = occurredAt
        private set

    fun update(stackTrace: JvmStackTrace, occurredAt: Instant) {
        this.stackTrace = stackTrace
        this.occurredAt = occurredAt
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
        update((artifactAdvice as ActiveExceptionAdvice).stackTrace, artifactAdvice.occurredAt)
    }

    override fun toString(): String {
        return "$type{$artifact - ${stackTrace.exceptionType} @ $occurredAt}"
    }
}
