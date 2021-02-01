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
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogFollowCommand(
    private val repository: Repository,
    private var artifact: LocalArtifact,
    private var forward: Boolean = false,
    private var targetCommitId: String
) {

    private var git: Git? = null
    private var currentPath: String? = null
    private var nextStart: ObjectId? = null

    @Throws(IOException::class, MissingObjectException::class, GitAPIException::class)
    fun call(): List<LocalArtifact> {
        val commits = ArrayList<RevCommit>()
        git = Git(repository)
        var start: ObjectId? = null

        nextStart = ObjectId.fromString(artifact.artifactQualifiedName.commitId)
        val artifacts = mutableListOf<LocalArtifact>()
        currentPath = artifact.filePath
        do {
            if (forward) {
                commits.clear()
                start = null
            } //todo: make sure can't go past artifact.commitId

            val log = git!!.log()
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
        } while (getRenamedPath(start as RevCommit, if (forward) commits else git!!.log().add(start).call()).also {
                if (it != null) {
                    artifacts.add(it)
                    currentPath = it.filePath
                }
            } != null)
        git?.close()
        return artifacts
    }

    @Throws(IOException::class, MissingObjectException::class, GitAPIException::class)
    private fun getRenamedPath(start: RevCommit, commits: Iterable<RevCommit>): LocalArtifact? {
        for (commit in commits) {
            val tw = TreeWalk(repository)
            if (forward) {
                tw.addTree(start.tree)
                tw.addTree(commit.tree)
            } else {
//                tw.addTree(commit.tree)
//                tw.addTree(start.tree)

                tw.addTree(start.tree)
                tw.addTree(commit.tree)
            }
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
