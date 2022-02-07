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
package spp.jetbrains.mapper.integration

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Constants
import org.intellij.lang.annotations.Language
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import spp.jetbrains.mapper.api.impl.SourceMapperImpl
import spp.jetbrains.mapper.extend.SourceCodeTokenizer
import spp.jetbrains.mapper.vcs.git.GitRepositoryMapper
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Ignore
class KotlinModificationTest {

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
            @Language("Kt") val code = """
                class GetterMethod {
                    private val str: String
                    fun getStr(): String {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.kt").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Kt") val renamedCode = """
                class GetterMethod {
                    private val str: String
                    fun getStr2(): String {
                        return str
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.kt").writeText(renamedCode)
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
            @Language("Kt") val code = """
                class GetterMethod {
                    fun getStr() {
                        System.currentTimeMillis()
                    }
                    fun getStr2() {
                        System.gc()
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.kt").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Kt") val renamedCode = """
                class GetterMethod {
                    fun getStr3() {
                        System.currentTimeMillis()
                    }
                    fun getStr4() {
                        System.gc()
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.kt").writeText(renamedCode)
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
            @Language("Kt") val code = """
                class GetterMethod {
                    fun getStr() {
                        System.currentTimeMillis()
                    }
                    fun getStr2() {
                        System.gc()
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.kt").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()

            @Language("Kt") val renamedCode = """
                class GetterMethod {
                    fun getStr3() {
                        System.currentTimeMillis()
                    }
                }
            """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.kt").writeText(renamedCode)
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
}
