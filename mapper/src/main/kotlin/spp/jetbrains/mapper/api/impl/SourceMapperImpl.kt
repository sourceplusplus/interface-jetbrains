/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.mapper.api.impl

import org.slf4j.LoggerFactory
import spp.jetbrains.mapper.api.SourceMapper
import spp.jetbrains.mapper.vcs.git.GitRepositoryMapper
import spp.jetbrains.mapper.vcs.git.LogFollowCommand
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.LocalArtifact
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
