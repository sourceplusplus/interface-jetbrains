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
package spp.jetbrains.mapper.vcs.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY
import org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME
import org.eclipse.jgit.diff.RenameDetector
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import org.slf4j.LoggerFactory
import spp.jetbrains.mapper.api.impl.SourceMapperImpl
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import spp.protocol.artifact.LocalArtifact
import java.io.IOException

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogFollowCommand(
    private val repository: Repository,
    private var artifact: LocalArtifact,
    private var targetCommitId: String
) {

    companion object {
        private val log = LoggerFactory.getLogger(SourceMapperImpl::class.java)
    }

    private var git = Git(repository)
    private var currentPath: String? = null
    private var nextStart: ObjectId? = null
    private var forward: Boolean = true

    @Throws(IOException::class, MissingObjectException::class, GitAPIException::class)
    fun call(): List<LocalArtifact> {
        log.info(
            "Executing log follow command. Repository: {} - Artifact: {} - Target commit id: {}",
            repository, artifact, targetCommitId
        )

        //determine direction
        val originalStart = ObjectId.fromString(artifact.artifactQualifiedName.commitId)
        val startCommitWalk = git.log().add(originalStart).call()
        forward = !startCommitWalk.any { it.id.name == targetCommitId }
        log.debug("Forward: {}", forward)

        //follow artifact
        val artifacts = mutableListOf<LocalArtifact>()
        val commits = ArrayList<RevCommit>()
        var start: ObjectId? = null
        nextStart = originalStart
        currentPath = artifact.filePath
        do {
            if (forward) {
                commits.clear()
                start = null
            }

            val logCommits = git.log()
                .add(nextStart).add(ObjectId.fromString(targetCommitId))
                .addPath(currentPath).call().toList()
            log.info("Found commits: {}", logCommits.joinToString())

            for (commit in (if (forward) logCommits.asReversed() else logCommits)) {
                if (commits.contains(commit)) {
                    start = null
                } else {
                    start = if (forward && artifacts.isNotEmpty()) {
                        nextStart
                    } else {
                        commit
                    }
                    commits.add(commit)
                }
            }
            if (start == null) return artifacts
        } while (getRenamedPath(start as RevCommit, if (forward) commits else git.log().add(start).call()).also {
                if (it != null) {
                    artifacts.add(it)
                    currentPath = it.filePath
                }
            } != null)
        git.close()
        return artifacts
    }

    @Throws(IOException::class, MissingObjectException::class, GitAPIException::class)
    private fun getRenamedPath(start: RevCommit, commits: Iterable<RevCommit>): LocalArtifact? {
        for (commit in commits) {
            val tw = TreeWalk(repository)
            tw.addTree(start.tree)
            tw.addTree(commit.tree)
            tw.isRecursive = true

            val rd = RenameDetector(repository)
            rd.addAll(DiffEntry.scan(tw))
            for (diff in rd.compute()) {
                if ((diff.changeType == RENAME || diff.changeType == COPY)) {
                    if (forward && diff.oldPath.contains(currentPath!!)) {
                        nextStart = commit
                        return LocalArtifact(
                            ArtifactQualifiedName(
                                diff.newPath.substring(0, diff.newPath.lastIndexOf(".")),
                                commit.name,
                                ArtifactType.METHOD
                            ),
                            diff.newPath
                        )
                    }
                    if (!forward && diff.oldPath.contains(currentPath!!)) {
                        return LocalArtifact(
                            ArtifactQualifiedName(
                                diff.newPath.substring(0, diff.newPath.lastIndexOf(".")),
                                commit.name,
                                ArtifactType.METHOD
                            ),
                            diff.newPath
                        )
                    }
                }
            }
        }
        return null
    }
}
