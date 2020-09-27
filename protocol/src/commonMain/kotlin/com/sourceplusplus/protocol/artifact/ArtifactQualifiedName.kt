package com.sourceplusplus.protocol.artifact

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class ArtifactQualifiedName(
    val identifier: String,
    val commitId: String,
    val type: ArtifactType,
    val operationName: String? = null //todo: only method artifacts need
)
