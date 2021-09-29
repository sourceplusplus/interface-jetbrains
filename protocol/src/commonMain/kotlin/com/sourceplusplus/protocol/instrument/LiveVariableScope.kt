package com.sourceplusplus.protocol.instrument

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
enum class LiveVariableScope {
    LOCAL_VARIABLE,
    INSTANCE_FIELD,
    STATIC_FIELD,
    GENERATED_METHOD
}
