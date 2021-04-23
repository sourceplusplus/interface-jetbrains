package com.sourceplusplus.mapper.api.impl

import com.sourceplusplus.mapper.api.SourceMapper
import com.sourceplusplus.mapper.vcs.git.GitRepositoryMapper
import com.sourceplusplus.mapper.vcs.git.LogFollowCommand
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.LocalArtifact
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMapperImpl(private val mapper: GitRepositoryMapper) : SourceMapper {

    override fun getMethodQualifiedName(
        methodQualifiedName: ArtifactQualifiedName,
        targetCommitId: String,
        returnBestEffort: Boolean
    ): Optional<ArtifactQualifiedName> {
        val methodRenames = LogFollowCommand(
            mapper.targetRepo, LocalArtifact(
                methodQualifiedName, mapper.cacheThing[methodQualifiedName.identifier]!!
            ), targetCommitId
        ).call()
        val possibleName = methodRenames.lastOrNull()
        return if (possibleName != null
            && (possibleName.artifactQualifiedName.commitId == targetCommitId || returnBestEffort)
        ) {
            Optional.of(possibleName.artifactQualifiedName)
        } else {
            Optional.ofNullable(null)
        }
    }
}
