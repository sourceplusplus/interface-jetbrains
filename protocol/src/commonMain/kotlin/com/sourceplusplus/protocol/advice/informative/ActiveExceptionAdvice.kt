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
    override val artifact: ArtifactQualifiedName,
    val stackTrace: JvmStackTrace
) : ArtifactAdvice {

    override val category: AdviceCategory = AdviceCategory.INFORMATIVE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActiveExceptionAdvice) return false
        if (artifact != other.artifact) return false
        if (stackTrace != other.stackTrace) return false
        return true
    }

    override fun hashCode(): Int {
        var result = artifact.hashCode()
        result = 31 * result + stackTrace.hashCode()
        return result
    }
}