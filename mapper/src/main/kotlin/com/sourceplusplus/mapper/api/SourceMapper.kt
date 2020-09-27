package com.sourceplusplus.mapper.api

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceMapper {

    //fun getCurrentMethodQualifiedName(methodQualifiedName: String, commitId: String): String

    /**
     * todo: description.
     */
    fun getMethodQualifiedName(
        methodQualifiedName: ArtifactQualifiedName,
        targetCommitId: String
    ): ArtifactQualifiedName
}
