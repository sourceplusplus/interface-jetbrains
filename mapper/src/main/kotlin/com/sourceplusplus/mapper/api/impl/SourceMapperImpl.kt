package com.sourceplusplus.mapper.api.impl

import com.google.common.base.Preconditions
import com.sourceplusplus.mapper.api.SourceMapper
import com.sourceplusplus.mapper.vcs.git.GitRepositoryMapper
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import org.eclipse.jgit.diff.RenameDetector
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMapperImpl(private val mapper: GitRepositoryMapper) : SourceMapper {

    override fun getMethodQualifiedName(
        methodQualifiedName: ArtifactQualifiedName,
        targetCommitId: String
    ): ArtifactQualifiedName {
        var diffs = mapper.targetGit.diff()
            .setOldTree(prepareTreeParser(mapper.targetRepo, methodQualifiedName.commitId))
            .setNewTree(prepareTreeParser(mapper.targetRepo, targetCommitId))
//            .setPathFilter(PathFilterGroup.createFromStrings("new/b.txt", "b.txt")) //todo: to path
            .call()
        val rd = RenameDetector(mapper.targetRepo)
        rd.addAll(diffs)
        diffs = rd.compute()
        return if (diffs.isNotEmpty()) {
            Preconditions.checkArgument(diffs.size == 1)
            methodQualifiedName.copy(
                identifier = parseFinerGitName(diffs[0].newPath),
                commitId = targetCommitId
            )
        } else {
            methodQualifiedName.copy(commitId = targetCommitId)
        }
    }

    private fun prepareTreeParser(repository: Repository, objectId: String): AbstractTreeIterator {
        RevWalk(repository).use { walk ->
            walk.revFilter = RevFilter.NO_MERGES

            val commit: RevCommit = walk.parseCommit(repository.resolve(objectId))
            val tree: RevTree = walk.parseTree(commit.tree.id)
            val treeParser = CanonicalTreeParser()
            repository.newObjectReader().use { reader -> treeParser.reset(reader, tree.id) }
            walk.dispose()
            return treeParser
        }
    }

    private fun parseFinerGitName(name: String): String {
        return name.substring(0, name.indexOf(".mjava"))
    }
}
