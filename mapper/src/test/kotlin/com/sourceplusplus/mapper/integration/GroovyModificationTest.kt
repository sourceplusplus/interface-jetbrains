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
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class GroovyModificationTest {

    @Test
    fun singleModifiedFile() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
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

        Git.init().setDirectory(tmpRepo).call().use { git ->
            @Language("Groovy") val code = """
                class GetterMethod {
                    private String str
                    String getStr() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Groovy") val renamedCode = """
                class GetterMethod {
                    private String str
                    String getStr2() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(renamedCode)
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
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
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

        Git.init().setDirectory(tmpRepo).call().use { git ->
            @Language("Groovy") val code = """
                class GetterMethod {
                    void getStr() {
                        System.currentTimeMillis()
                    }
                    void getStr2() {
                        System.gc()
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Groovy") val renamedCode = """
                class GetterMethod {
                    void getStr3() {
                        System.currentTimeMillis()
                    }
                    void getStr4() {
                        System.gc()
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(renamedCode)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed methods").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

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
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
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

        Git.init().setDirectory(tmpRepo).call().use { git ->
            @Language("Groovy") val code = """
                class GetterMethod {
                    void getStr() {
                        System.currentTimeMillis()
                    }
                    void getStr2() {
                        System.gc()
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Groovy") val renamedCode = """
                class GetterMethod {
                    void getStr3() {
                        System.currentTimeMillis()
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(renamedCode)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed methods").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

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

    @Test
    fun methodRenamedTwice() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
        val getCount = AtomicInteger()
        val sourceCodeTokenizer = object : SourceCodeTokenizer {
            override fun getMethods(filename: String, sourceCode: String): List<SourceCodeTokenizer.TokenizedMethod> {
                getCount.getAndAdd(1)
                if (getCount.get() == 1) {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr()",
                                "1",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                } else if (getCount.get() == 2) {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr2()",
                                "2",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                } else {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr3()",
                                "3",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                }
            }
        }

        Git.init().setDirectory(tmpRepo).call().use { git ->
            @Language("Groovy") var code = """
                class GetterMethod {
                    private String str
                    String getStr() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            code = """
                class GetterMethod {
                    private String str
                    String getStr2() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed method").call()

            code = """
                class GetterMethod {
                    private String str
                    String getStr3() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed method again").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = gitMapper.targetRepo.resolve("$newCommitId~2").name
        val oldName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr()",
            commitId = oldCommitId,
            type = ArtifactType.METHOD
        )
        val newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)
        assertNotNull(newName)
        assertTrue(newName.isPresent)
        assertEquals("GetterMethod.getStr3()", newName.get().identifier)
        assertEquals(newCommitId, newName.get().commitId)
        assertEquals("GetterMethod.getStr()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)

        val foundOldName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(newName.get(), oldCommitId)
        assertNotNull(foundOldName)
        assertTrue(foundOldName.isPresent)
        assertEquals("GetterMethod.getStr()", foundOldName.get().identifier)
        assertEquals(oldCommitId, foundOldName.get().commitId)

        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }

    @Test
    fun methodRenamedThenDeleted() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
        val getCount = AtomicInteger()
        val sourceCodeTokenizer = object : SourceCodeTokenizer {
            override fun getMethods(filename: String, sourceCode: String): List<SourceCodeTokenizer.TokenizedMethod> {
                getCount.getAndAdd(1)
                if (getCount.get() == 1) {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr()",
                                "1",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                } else if (getCount.get() == 2) {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr2()",
                                "2",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                } else {
                    return emptyList()
                }
            }
        }

        Git.init().setDirectory(tmpRepo).call().use { git ->
            @Language("Groovy") var code = """
                class GetterMethod {
                    private String str
                    String getStr() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            code = """
                class GetterMethod {
                    private String str
                    String getStr2() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed method").call()

            code = """
                class GetterMethod {
                    private String str
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Deleted method").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val oldCommitId = gitMapper.targetRepo.resolve("$newCommitId~2").name
        val oldName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr()",
            commitId = oldCommitId,
            type = ArtifactType.METHOD
        )
        val newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId)

        assertNotNull(newName)
        assertFalse(newName.isPresent)
        assertEquals("GetterMethod.getStr()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)
        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }

    @Test
    fun methodRenamedThenDeleted_getBestEffort() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
        val getCount = AtomicInteger()
        val sourceCodeTokenizer = object : SourceCodeTokenizer {
            override fun getMethods(filename: String, sourceCode: String): List<SourceCodeTokenizer.TokenizedMethod> {
                getCount.getAndAdd(1)
                if (getCount.get() == 1) {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr()",
                                "1",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                } else if (getCount.get() == 2) {
                    return listOf(
                        SourceCodeTokenizer.TokenizedMethod(
                            ArtifactQualifiedName(
                                "GetterMethod.getStr2()",
                                "2",
                                ArtifactType.METHOD
                            ), listOf("")
                        )
                    )
                } else {
                    return emptyList()
                }
            }
        }

        Git.init().setDirectory(tmpRepo).call().use { git ->
            @Language("Groovy") var code = """
                class GetterMethod {
                    private String str
                    String getStr() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            code = """
                class GetterMethod {
                    private String str
                    String getStr2() {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Renamed method").call()

            code = """
                class GetterMethod {
                    private String str
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.groovy").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Deleted method").call()
        }

        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(FileRepository(File(tmpRepo, ".git")))

        val newCommitId = gitMapper.targetRepo.resolve(Constants.HEAD).name
        val secondCommitId = gitMapper.targetRepo.resolve("$newCommitId~1").name
        val oldCommitId = gitMapper.targetRepo.resolve("$newCommitId~2").name
        val oldName = ArtifactQualifiedName(
            identifier = "GetterMethod.getStr()",
            commitId = oldCommitId,
            type = ArtifactType.METHOD
        )
        val newName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(oldName, newCommitId, true)

        assertNotNull(newName)
        assertTrue(newName.isPresent)
        assertEquals("GetterMethod.getStr2()", newName.get().identifier)
        assertEquals(secondCommitId, newName.get().commitId)
        assertEquals("GetterMethod.getStr()", oldName.identifier)
        assertEquals(oldCommitId, oldName.commitId)

        val foundOldName = SourceMapperImpl(gitMapper)
            .getMethodQualifiedName(
                ArtifactQualifiedName(
                    identifier = "GetterMethod.getStr2()",
                    commitId = secondCommitId,
                    type = ArtifactType.METHOD
                ), oldCommitId
            )
        assertNotNull(foundOldName)
        assertTrue(foundOldName.isPresent)
        assertEquals("GetterMethod.getStr()", foundOldName.get().identifier)
        assertEquals(oldCommitId, foundOldName.get().commitId)

        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }
}
