package com.sourceplusplus.mapper.vcs.git

import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.LocalArtifact
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
import java.io.IOException
import java.util.*

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

    private var git = Git(repository)
    private var currentPath: String? = null
    private var nextStart: ObjectId? = null
    private var forward: Boolean = true

    @Throws(IOException::class, MissingObjectException::class, GitAPIException::class)
    fun call(): List<LocalArtifact> {
        //determine direction
        val originalStart = ObjectId.fromString(artifact.artifactQualifiedName.commitId)
        val startCommitWalk = git.log().add(originalStart).call()
        forward = !startCommitWalk.any { it.id.name == targetCommitId }

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

            val log = git.log()
                .add(nextStart).add(ObjectId.fromString(targetCommitId))
                .addPath(currentPath).call()
            for (commit in (if (forward) log.toList().asReversed() else log)) {
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
