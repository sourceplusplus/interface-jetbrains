package com.sourceplusplus.mapper.vcs.git

import com.sourceplusplus.mapper.SourceMapperTest
import com.sourceplusplus.mapper.api.impl.SourceMapperImpl
import com.sourceplusplus.mapper.vcs.git.GitRepositoryMapper.Companion.originalCommitIdPattern
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.*

@Disabled
class MethodRenameTest : SourceMapperTest() {

    @Test
    fun `java get original method name`() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
        Git.init().setDirectory(tmpRepo).call().use { git ->
            @Language("Java") val code = """
                public class GetterMethod {
                    private String str;
                    public String getStr() {
                        return str;
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.java").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Java") val renamedCode = """
                public class GetterMethod {
                    private String str;
                    public String getStr2() {
                        return str;
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.java").writeText(renamedCode)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed method").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = gitMapper.targetRepo.resolve("$newCommitId^1").name
        val oldName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr()",
            commitId = oldCommitId,
            type = ArtifactType.METHOD
        )
        val newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)

        assertTrue(newName.isPresent)
        assertEquals("GetterMethod.getStr2()", newName.get().identifier)
        assertEquals(newCommitId, newName.get().commitId)
        assertEquals("GetterMethod.getStr()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)
        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }

    @Test
    fun `java get updated method name`() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
        Git.init().setDirectory(tmpRepo).call().use { git ->
            @Language("Java") val code = """
                public class GetterMethod {
                    private String str;
                    public String getStr() {
                        return str;
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.java").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Java") val renamedCode = """
                public class GetterMethod {
                    private String str;
                    public String getStr2() {
                        return str;
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.java").writeText(renamedCode)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed method").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = gitMapper.targetRepo.resolve("$newCommitId^1").name
        val newName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr2()",
            commitId = newCommitId,
            type = ArtifactType.METHOD
        )
        val oldName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(newName, oldCommitId)

        assertTrue(oldName.isPresent)
        assertEquals("GetterMethod.getStr()", oldName.get().identifier)
        assertEquals(oldCommitId, oldName.get().commitId)
        assertEquals("GetterMethod.getStr2()", newName.identifier)
        assertEquals(newCommitId, newName.commitId)
        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }

    @Test
    fun `java get reinitialize original method name`() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        val git = Git.init().setDirectory(tmpRepo).call()
        @Language("Java") val code = """
            public class GetterMethod {
                private String str;
                public String getStr() {
                    return str;
                }
            }
            """.trimIndent()
        File(git.repository.directory.parent, "GetterMethod.java").writeText(code)
        git.add().addFilepattern(".").call()
        val initialCommit = git.commit().setMessage("Initial commit").call()

        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

        @Language("Java") val renamedCode = """
            public class GetterMethod {
                private String str;
                public String getStr2() {
                    return str;
                }
            }
            """.trimIndent()
        File(git.repository.directory.parent, "GetterMethod.java").writeText(renamedCode)
        git.add().addFilepattern(".").call()
        val lastCommit = git.commit().setMessage("Renamed method").call()

        gitMapper.targetGit.remoteAdd().setName("sourceRepo")
            .setUri(URIish(gitMapper.sourceRepo.directory.absolutePath))
            .call()
        gitMapper.targetGit.fetch().setRemote("sourceRepo").call()
        gitMapper.targetGit.merge()
            .include(gitMapper.targetRepo.resolve("refs/remotes/sourceRepo/master"))
            .call()
        gitMapper.reinitialize()

        val rw = RevWalk(gitMapper.targetRepo)
        val headId: AnyObjectId
        headId = gitMapper.targetRepo.resolve(Constants.HEAD)
        val root: RevCommit = rw.parseCommit(headId)
        rw.sort(RevSort.REVERSE)
        rw.markStart(root)
        val firstCommit = rw.next()

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = firstCommit.name
        val oldName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr()",
            commitId = oldCommitId,
            type = ArtifactType.METHOD
        )
        val newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)

        assertTrue(newName.isPresent)
        assertEquals("GetterMethod.getStr2()", newName.get().identifier)
        assertEquals(newCommitId, newName.get().commitId)
        assertEquals("GetterMethod.getStr()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)

        val sourceCommits = git.log().call().toList()
        val targetCommits = gitMapper.targetGit.log().call().toList()
        assertNotNull(sourceCommits)
        assertNotNull(targetCommits)
        assertEquals(2, sourceCommits.size)
        assertEquals(sourceCommits.size, targetCommits.size)

        val matcher0 = originalCommitIdPattern.matcher(targetCommits[0].fullMessage)
        val matcher1 = originalCommitIdPattern.matcher(targetCommits[1].fullMessage)
        assertTrue(matcher0.find())
        assertTrue(matcher1.find())
        assertEquals(lastCommit.id.name, sourceCommits[0].id.name)
        assertEquals(initialCommit.id.name, sourceCommits[1].id.name)
        assertEquals(sourceCommits[0].id.name, matcher0.group(1))
        assertEquals(sourceCommits[1].id.name, matcher1.group(1))

        git.close()
        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }

    @Test
    fun `java get reinitialize updated method name`() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        val git = Git.init().setDirectory(tmpRepo).call()
        @Language("Java") val code = """
            public class GetterMethod {
                private String str;
                public String getStr() {
                    return str;
                }
            }
            """.trimIndent()
        File(git.repository.directory.parent, "GetterMethod.java").writeText(code)
        git.add().addFilepattern(".").call()
        val initialCommit = git.commit().setMessage("Initial commit").call()

        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

        @Language("Java") val renamedCode = """
            public class GetterMethod {
                private String str;
                public String getStr2() {
                    return str;
                }
            }
            """.trimIndent()
        File(git.repository.directory.parent, "GetterMethod.java").writeText(renamedCode)
        git.add().addFilepattern(".").call()
        val lastCommit = git.commit().setMessage("Renamed method").call()

        gitMapper.targetGit.remoteAdd().setName("sourceRepo")
            .setUri(URIish(gitMapper.sourceRepo.directory.absolutePath))
            .call()
        gitMapper.targetGit.fetch().setRemote("sourceRepo").call()
        gitMapper.targetGit.merge()
            .include(gitMapper.targetRepo.resolve("refs/remotes/sourceRepo/master"))
            .call()
        gitMapper.reinitialize()

        val rw = RevWalk(gitMapper.targetRepo)
        val headId: AnyObjectId
        headId = gitMapper.targetRepo.resolve(Constants.HEAD)
        val root: RevCommit = rw.parseCommit(headId)
        rw.sort(RevSort.REVERSE)
        rw.markStart(root)
        val firstCommit = rw.next()

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = firstCommit.name
        val newName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr2()",
            commitId = newCommitId,
            type = ArtifactType.METHOD
        )
        val oldName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(newName, oldCommitId)

        assertTrue(oldName.isPresent)
        assertEquals("GetterMethod.getStr()", oldName.get().identifier)
        assertEquals(oldCommitId, oldName.get().commitId)
        assertEquals("GetterMethod.getStr2()", newName.identifier)
        assertEquals(newCommitId, newName.commitId)

        val sourceCommits = git.log().call().toList()
        val targetCommits = gitMapper.targetGit.log().call().toList()
        assertNotNull(sourceCommits)
        assertNotNull(targetCommits)
        assertEquals(2, sourceCommits.size)
        assertEquals(sourceCommits.size, targetCommits.size)

        val matcher0 = originalCommitIdPattern.matcher(targetCommits[0].fullMessage)
        val matcher1 = originalCommitIdPattern.matcher(targetCommits[1].fullMessage)
        assertTrue(matcher0.find())
        assertTrue(matcher1.find())
        assertEquals(lastCommit.id.name, sourceCommits[0].id.name)
        assertEquals(initialCommit.id.name, sourceCommits[1].id.name)
        assertEquals(sourceCommits[0].id.name, matcher0.group(1))
        assertEquals(sourceCommits[1].id.name, matcher1.group(1))

        git.close()
        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }
}
