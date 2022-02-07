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
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import spp.jetbrains.mapper.SourceMapperTest
import java.io.File
import java.nio.file.Files
import java.util.*

class GitRepositoryMapperTest : SourceMapperTest() {

    @Test
    fun `tokenized java getter method`() {
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
        }

        val fileRepo = FileRepository(File(tmpRepo, ".git"))
        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(fileRepo)

        val finerMethodFile = File(gitMapper.targetSourceDirectory, "GetterMethod.getStr().mjava")
        assertExists(finerMethodFile)
        assertEquals(
            """
            public
            String
            getStr
            (
            )
            {
            return
            str
            ;
            }
            """.trimIndent(), finerMethodFile.readText().trimIndent()
        )
        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }

    @Test
    fun `tokenized groovy getter method`() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
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
        }

        val fileRepo = FileRepository(File(tmpRepo, ".git"))
        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(fileRepo)

        val finerMethodFile = File(gitMapper.targetSourceDirectory, "GetterMethod.getStr().mgroovy")
        assertExists(finerMethodFile)
        assertEquals(
            """
            String
            getStr
            (
            )
            {
            return
            str
            }
            """.trimIndent(), finerMethodFile.readText().trimIndent()
        )
        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }

//    @Test
//    fun `tokenized kotlin getter method`() {
//        Git.init().setDirectory(File("/tmp/git-repo")).call().use { git ->
//            @Language("Kt") val code = """
//                class GetterMethod(private val str: String) {
//                    fun getStr(): String {
//                        return str
//                    }
//                }
//                """.trimIndent()
//            File(git.repository.directory.parent, "GetterMethod.kt").writeText(code)
//            git.add().addFilepattern(".").call()
//            git.commit().setMessage("Initial commit").call()
//        }
//
//        val fileRepo = FileRepository("/tmp/git-repo/.git")
//        val gitMapper = GitRepositoryMapper(project)
//        gitMapper.initialize(fileRepo)
//
//        val finerMethodFile = File(gitMapper.targetSourceDirectory, "GetterMethod.getStr().mkt")
//        assertExists(finerMethodFile)
//        assertEquals(
//            """
//            fun
//            getStr
//            (
//            )
//            {
//            return
//            str
//            }
//            """.trimIndent(), finerMethodFile.readText().trimIndent()
//        )
//        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
//        gitMapper.targetGit.close()
//        gitMapper.targetSourceDirectory.deleteRecursively()
//    }

    @Disabled
    @Test
    fun `tokenized scala getter method`() {
        val tmpRepo = Files.createTempDirectory("test-" + UUID.randomUUID()).toFile()
        Git.init().setDirectory(tmpRepo).call().use { git ->
            //todo: @Language("Scala") doesn't work
            @Language("Scala") val code = """
                class GetterMethod {
                  private var str: String = _
                
                  def getStr(): String = {
                    str
                  }
                }
                """.trimIndent()
            File(git.repository.directory.parent, "GetterMethod.scala").writeText(code)
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()
        }

        val fileRepo = FileRepository(File(tmpRepo, ".git"))
        val gitMapper = GitRepositoryMapper(sourceCodeTokenizer)
        gitMapper.initialize(fileRepo)

        val finerMethodFile = File(gitMapper.targetSourceDirectory, "GetterMethod.getStr().mscala")
        assertExists(finerMethodFile)
        assertEquals(
            """
            def
            getStr
            (
            )
            :
            String
            =
            {
            str
            }
            """.trimIndent(), finerMethodFile.readText().trimIndent()
        )
        gitMapper.sourceRepo.directory.parentFile.deleteRecursively()
        gitMapper.targetGit.close()
        gitMapper.targetSourceDirectory.deleteRecursively()
    }
}
