package com.sourceplusplus.protocol.artifact

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
enum class ArtifactType {
    CLASS,
    METHOD,
    STATEMENT,
    EXPRESSION,
    ENDPOINT
}
