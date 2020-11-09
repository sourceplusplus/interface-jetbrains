package com.sourceplusplus.protocol.advice

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class ArtifactAdvice(
    val artifact: ArtifactQualifiedName,
    val category: AdviceCategory,
    val type: AdviceType
) {

    private val artifactAdviceListeners = mutableListOf<ArtifactAdviceListener>()

    fun addArtifactAdviceListener(artifactAdviceListener: ArtifactAdviceListener) {
        artifactAdviceListeners.add(artifactAdviceListener)
    }

    protected fun triggerUpdated() {
        artifactAdviceListeners.forEach(ArtifactAdviceListener::updated)
    }

    /**
     * Determine if [artifactAdvice] is the same as this one. If so, the first [ArtifactAdvice] should be updated.
     */
    abstract fun isSameArtifactAdvice(artifactAdvice: ArtifactAdvice): Boolean

    abstract fun updateArtifactAdvice(artifactAdvice: ArtifactAdvice)
}
