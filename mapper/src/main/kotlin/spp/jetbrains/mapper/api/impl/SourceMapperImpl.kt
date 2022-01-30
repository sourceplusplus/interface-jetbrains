package spp.jetbrains.mapper.api.impl

import spp.jetbrains.mapper.api.SourceMapper
import spp.jetbrains.mapper.vcs.git.GitRepositoryMapper
import spp.jetbrains.mapper.vcs.git.LogFollowCommand
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.LocalArtifact
import org.slf4j.LoggerFactory
import java.util.*

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMapperImpl(private val mapper: GitRepositoryMapper) : SourceMapper {

    companion object {
        private val log = LoggerFactory.getLogger(SourceMapperImpl::class.java)
    }

    override fun getMethodQualifiedName(
        methodQualifiedName: ArtifactQualifiedName,
        targetCommitId: String,
        returnBestEffort: Boolean
    ): Optional<ArtifactQualifiedName> {
        log.debug(
            "Getting method qualified name: {} - Target commit id: {} - Best effort: {}",
            methodQualifiedName, targetCommitId, returnBestEffort
        )
        val methodRenames = LogFollowCommand(
            mapper.targetRepo, LocalArtifact(
                methodQualifiedName, mapper.cacheThing[methodQualifiedName.identifier]!!
            ), targetCommitId
        ).call()
        log.info("Method renames: {}", methodRenames.joinToString())

        val possibleName = methodRenames.lastOrNull()
        return if (possibleName?.artifactQualifiedName?.commitId == targetCommitId || returnBestEffort) {
            Optional.ofNullable(possibleName?.artifactQualifiedName)
        } else {
            Optional.ofNullable(null)
        }
    }
}
