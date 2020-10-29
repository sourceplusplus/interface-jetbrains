package com.sourceplusplus.protocol.artifact

import com.sourceplusplus.protocol.Serializers.ArtifactTypeSerializer
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
    @Serializable(with = ArtifactTypeSerializer::class)
    val type: ArtifactType,
    val lineNumber: Int? = null,
    val operationName: String? = null //todo: only method artifacts need
) {
//    val qualifiedClassName: String?
//        get() = ArtifactNameUtils.getQualifiedClassName(identifier)
//    val qualifiedFunctionName: String?
//        get() = ArtifactNameUtils.getFunctionSignature(identifier)
}
