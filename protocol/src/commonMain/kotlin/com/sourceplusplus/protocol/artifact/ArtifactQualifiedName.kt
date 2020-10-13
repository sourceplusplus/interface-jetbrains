package com.sourceplusplus.protocol.artifact

import com.sourceplusplus.protocol.ArtifactNameUtils

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
    val lineNumber: Int? = null,
    val operationName: String? = null //todo: only method artifacts need
) {
//    val qualifiedClassName: String?
//        get() = ArtifactNameUtils.getQualifiedClassName(identifier)
//    val qualifiedFunctionName: String?
//        get() = ArtifactNameUtils.getFunctionSignature(identifier)
}
