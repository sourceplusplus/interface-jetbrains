package com.sourceplusplus.protocol.advice

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface AdviceListener {
    suspend fun advised(advice: ArtifactAdvice)
}
