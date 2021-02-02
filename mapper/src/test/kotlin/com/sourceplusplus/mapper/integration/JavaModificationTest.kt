package com.sourceplusplus.mapper.integration

import com.sourceplusplus.mapper.api.impl.SourceMapperImpl
import com.sourceplusplus.mapper.extend.SourceCodeTokenizer
import com.sourceplusplus.mapper.vcs.git.GitRepositoryMapper
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Constants
import org.intellij.lang.annotations.Language
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class JavaModificationTest {

    @Before
    fun setup() {
        if (File("/tmp/git-repo").exists()) {
            File("/tmp/git-repo").deleteRecursively()
        }
    }

    @Test
    fun singleModifiedFile() {
        val first = AtomicBoolean()
        val sourceCodeTokenizer = object : SourceCodeTokenizer {
            override fun getMethods(filename: String, sourceCode: String): List<SourceCodeTokenizer.TokenizedMethod> {
                if (!first.get()) {
                    first.set(true)
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr()",
                                "1",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                } else {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr2()",
                                "2",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                }
            }
        }

        Git.init().setDirectory(File("/tmp/git-repo")).call().use { git ->
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
        gitMapper.initialize(FileRepository("/tmp/git-repo/.git"))

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = gitMapper.targetRepo.resolve("$newCommitId^1").name
        val oldName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr()",
            commitId = oldCommitId,
            type = ArtifactType.METHOD
        )
        val newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)

        assertNotNull(newName)
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
    fun twoModifiedFiles() {
        val first = AtomicBoolean()
        val sourceCodeTokenizer = object : SourceCodeTokenizer {
            override fun getMethods(filename: String, sourceCode: String): List<SourceCodeTokenizer.TokenizedMethod> {
                if (!first.get()) {
                    first.set(true)
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr()",
                                "1",
                                ArtifactType.METHOD
                            ), listOf("System", "currentTimeMillis()")
                        ),
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr2()",
                                "1",
                                ArtifactType.METHOD
                            ), listOf("System", "gc()")
                        )
                    )
                } else {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr3()",
                                "2",
                                ArtifactType.METHOD
                            ), listOf("System", "currentTimeMillis()")
                        ),
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr4()",
                                "2",
                                ArtifactType.METHOD
                            ), listOf("System", "gc()")
                        )
                    )
                }
            }
        }

        Git.init().setDirectory(File("/tmp/git-repo")).call().use { git ->
            @Language("Java") val code = """
                public class GetterMethod {
                    public void getStr() {
                        System.currentTimeMillis();
                    }
                    public void getStr2() {
                        System.gc();
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.java").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Java") val renamedCode = """
                public class GetterMethod {
                    public void getStr3() {
                        System.currentTimeMillis();
                    }
                    public void getStr4() {
                        System.gc();
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.java").writeText(renamedCode)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed methods").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository("/tmp/git-repo/.git"))

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = gitMapper.targetRepo.resolve("$newCommitId^1").name
        var oldName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr()",
            commitId = oldCommitId,
            type = ArtifactType.METHOD
        )
        var newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)

        assertNotNull(newName)
        assertTrue(newName.isPresent)
        assertEquals("GetterMethod.getStr3()", newName.get().identifier)
        assertEquals(newCommitId, newName.get().commitId)
        assertEquals("GetterMethod.getStr()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)

        oldName = oldName.copy(identifier = "GetterMethod.getStr2()")
        newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)

        assertNotNull(newName)
        assertTrue(newName.isPresent)
        assertEquals("GetterMethod.getStr4()", newName.get().identifier)
        assertEquals(newCommitId, newName.get().commitId)
        assertEquals("GetterMethod.getStr2()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)

        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }

    @Test
    fun oneModifiedAndOneDeletedFile() {
        val first = AtomicBoolean()
        val sourceCodeTokenizer = object : SourceCodeTokenizer {
            override fun getMethods(filename: String, sourceCode: String): List<SourceCodeTokenizer.TokenizedMethod> {
                if (!first.get()) {
                    first.set(true)
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr()",
                                "1",
                                ArtifactType.METHOD
                            ), listOf("System", "currentTimeMillis()")
                        ),
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr2()",
                                "1",
                                ArtifactType.METHOD
                            ), listOf("System", "gc()")
                        )
                    )
                } else {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr3()",
                                "2",
                                ArtifactType.METHOD
                            ), listOf("System", "currentTimeMillis()")
                        )
                    )
                }
            }
        }

        Git.init().setDirectory(File("/tmp/git-repo")).call().use { git ->
            @Language("Java") val code = """
                public class GetterMethod {
                    public void getStr() {
                        System.currentTimeMillis();
                    }
                    public void getStr2() {
                        System.gc();
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.java").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Java") val renamedCode = """
                public class GetterMethod {
                    public void getStr3() {
                        System.currentTimeMillis();
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.java").writeText(renamedCode)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed methods").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository("/tmp/git-repo/.git"))

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = gitMapper.targetRepo.resolve("$newCommitId^1").name
        var oldName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr()",
            commitId = oldCommitId,
            type = ArtifactType.METHOD
        )
        var newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)

        assertNotNull(newName)
        assertTrue(newName.isPresent)
        assertEquals("GetterMethod.getStr3()", newName.get().identifier)
        assertEquals(newCommitId, newName.get().commitId)
        assertEquals("GetterMethod.getStr()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)

        oldName = oldName.copy(identifier = "GetterMethod.getStr2()")
        newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)

        assertNotNull(newName)
        assertFalse(newName.isPresent)
        assertEquals("GetterMethod.getStr2()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)

        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }
}
