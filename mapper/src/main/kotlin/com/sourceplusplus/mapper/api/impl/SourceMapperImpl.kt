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
        returnBestEffort: Boolean,
        forward: Boolean
    ): Optional<ArtifactQualifiedName> {
        val methodRenames = LogFollowCommand(
            mapper.targetRepo, LocalArtifact(
                methodQualifiedName, mapper.cacheThing[methodQualifiedName.identifier]!!
            ), forward, targetCommitId //todo: determine if target id is before or after (set forward)
        ).call()
        val possibleName = methodRenames.lastOrNull()
        return if (possibleName != null && possibleName.artifactQualifiedName.commitId == targetCommitId) {
            Optional.of(possibleName.artifactQualifiedName)
        } else if (returnBestEffort && methodRenames.isNotEmpty()){
            Optional.of(methodRenames.last().artifactQualifiedName)
        } else {
            Optional.ofNullable(null)
        }
    }
}
