package com.sourceplusplus.mapper.vcs.git

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.io.File

class GitRepositoryMapperTest : LightPlatformCodeInsightFixture4TestCase() {

    @Test
    fun `tokenized java getter method`() {
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
        }

        val fileRepo = FileRepository("/tmp/git-repo/.git")
        val gitMapper = GitRepositoryMapper(project)
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
        Git.init().setDirectory(File("/tmp/git-repo")).call().use { git ->
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

        val fileRepo = FileRepository("/tmp/git-repo/.git")
        val gitMapper = GitRepositoryMapper(project)
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
//        if (File("/tmp/git-repo").exists()) File("/tmp/git-repo").deleteRecursively()
//        Git.init().setDirectory(File("/tmp/git-repo")).call().use { git ->
//            @Language("Kt") val code = """
//                class GetterMethod(private val str: String) {
//                    fun getStr(): String {
//                        return str
//                    }
//                }
//            """.trimIndent()
//            File(git.repository.directory.parent, "GetterMethod.kt").writeText(code)
//            git.add().addFilepattern(".").call()
//            git.commit().setMessage("Initial commit").call()
//        }
//
//        val fileRepo = FileRepository("/tmp/git-repo/.git")
//        val mapper = GitRepositoryMapper(project)
//        mapper.initialize(fileRepo, fileRepo)
//        mapper.rewrite(Context.init())
//        PorcelainAPI(fileRepo).use {
//            it.resetHard()
//            it.clean()
//        }
//
//        val finerMethodFile = File("/tmp/git-repo/GetterMethod.getStr().mkt")
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
//            ;
//            }
//        """.trimIndent(), finerMethodFile.readText().trimIndent()
//        )
//    }

    @Test
    fun `tokenized scala getter method`() {
        Git.init().setDirectory(File("/tmp/git-repo")).call().use { git ->
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

        val fileRepo = FileRepository("/tmp/git-repo/.git")
        val gitMapper = GitRepositoryMapper(project)
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
