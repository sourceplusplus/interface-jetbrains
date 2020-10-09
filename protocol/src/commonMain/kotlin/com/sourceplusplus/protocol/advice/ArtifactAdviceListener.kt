package com.sourceplusplus.protocol.advice

/**
 * Used to listen for when an [ArtifactAdvice] is updated.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
fun interface ArtifactAdviceListener {
    fun updated()
}
