package com.sourceplusplus.mapper.vcs.git

import com.sourceplusplus.mapper.SourceMapperTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.intellij.lang.annotations.Language
import org.junit.Ignore
import org.junit.Test
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

    @Ignore
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
