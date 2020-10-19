package com.sourceplusplus.protocol.artifact

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
data class ArtifactQualifiedName(
    val identifier: String,
    val commitId: String,
    val type: ArtifactType,
    val lineNumber: Int? = null,
    val operationName: String? = null //todo: only method artifacts need
) {
//    val qualifiedClassName: String?
//        get() = ArtifactNameUtils.getQualifiedClassName(identifier)
//    val qualifiedFunctionName: String?
//        get() = ArtifactNameUtils.getFunctionSignature(identifier)
}
