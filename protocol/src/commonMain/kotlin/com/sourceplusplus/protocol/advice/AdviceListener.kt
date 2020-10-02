package com.sourceplusplus.protocol.advice

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface AdviceListener {
    fun advised(advice: ArtifactAdvice)
}
