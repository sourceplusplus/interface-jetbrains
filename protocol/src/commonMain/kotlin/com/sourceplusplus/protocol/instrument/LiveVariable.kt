package com.sourceplusplus.protocol.instrument

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class LiveVariable(
    val name: String,
    @Contextual val value: Any,
    val lineNumber: Int = -1,
    val scope: LiveVariableScope? = null,
    val liveClazz: String? = null,
    val liveIdentity: String? = null
)
