/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.marker.jvm.detect

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import io.vertx.core.Vertx
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.uast.toUElement
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.source.SourceFileMarker
import java.io.File

class JVMLoggerDetectorTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : DefaultLightProjectDescriptor() {
            override fun getSdk(): Sdk {
                return when {
                    File("/usr/lib/jvm/java-11-openjdk-amd64").exists() -> {
                        JavaSdk.getInstance().createJdk(
                            "jdk-11", "/usr/lib/jvm/java-11-openjdk-amd64", false
                        )
                    }

                    File("/opt/java/openjdk").exists() -> {
                        JavaSdk.getInstance().createJdk(
                            "jdk-11", "/opt/java/openjdk", false
                        )
                    }

                    System.getenv("JAVA_HOME")?.let { File(it).exists() } == true -> {
                        JavaSdk.getInstance().createJdk(
                            "jdk-11", System.getenv("JAVA_HOME"), false
                        )
                    }

                    else -> {
                        error("Failed to find JDK 11")
                    }
                }
            }

            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)
                model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel = LanguageLevel.JDK_1_8
            }
        }
    }

    public override fun setUp() {
        super.setUp()

        myFixture.addClass(
            "package ch.qos.logback.classic;\n" +
                    "\n" +
                    "public class Logger {\n" +
                    "    public void trace(String msg){}\n" +
                    "    public void trace(String format, Object arg){}\n" +
                    "    public void trace(String format, Object arg1, Object arg2){}\n" +
                    "    public void trace(String format, Object... argArray){}\n" +
                    "    public void debug(String msg){}\n" +
                    "    public void debug(String format, Object arg){}\n" +
                    "    public void debug(String format, Object arg1, Object arg2){}\n" +
                    "    public void debug(String format, Object... argArray){}\n" +
                    "    public void info(String msg){}\n" +
                    "    public void info(String format, Object arg){}\n" +
                    "    public void info(String format, Object arg1, Object arg2){}\n" +
                    "    public void info(String format, Object... argArray){}\n" +
                    "    public void warn(String msg){}\n" +
                    "    public void warn(String format, Object arg){}\n" +
                    "    public void warn(String format, Object arg1, Object arg2){}\n" +
                    "    public void warn(String format, Object... argArray){}\n" +
                    "    public void error(String msg){}\n" +
                    "    public void error(String format, Object arg){}\n" +
                    "    public void error(String format, Object arg1, Object arg2){}\n" +
                    "    public void error(String format, Object... argArray){}\n" +
                    "    public void error(String msg, Throwable t){}\n" +
                    "}\n"
        )
    }

    override fun tearDown() {
        super.tearDown()
        TestApplicationManager.getInstance().setDataProvider(null)
    }

    fun testJavaLogbackLogger() {
        @Language("Java") val code = """
                    import ch.qos.logback.classic.Logger;
                    public class TestLogback {
                        private static final Logger log = new Logger();
                        public void loggers() {
                            log.trace("trace {}", "trace");
                            log.debug("debug {}", "debug");
                            log.info("info {}", "info");
                            log.warn("warn {}", "warn");
                            log.error("error {}", "error");
                        }
                    }
                """.trimIndent()

        ApplicationManager.getApplication().runReadAction {
            val sourceFile = myFixture.createFile("TestLogback.java", code).toPsiFile(project)
            assertNotNull(sourceFile)

            val psiFile = sourceFile as PsiJavaFile
            assertEquals(1, psiFile.classes.size)
            assertEquals(1, psiFile.classes[0].methods.size)

            JVMLanguageProvider().setup(project.apply { UserData.vertx(this, Vertx.vertx()) })
            SourceFileMarker.SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
            val fileMarker = SourceMarker.getSourceFileMarker(sourceFile)
            assertNotNull(fileMarker)

            val result = JVMLoggerDetector(project)
                .determineLoggerStatements(psiFile.classes[0].methods[0], fileMarker!!)
                .map { it.logPattern }
            assertEquals(5, result.size)
            assertContainsOrdered(result, "trace {}", "debug {}", "info {}", "warn {}", "error {}")
        }
    }

    fun testKotlinLogbackLogger() {
        @Language("kotlin") val code = """
                    import ch.qos.logback.classic.Logger
                    class TestLogback {
                        val log: Logger = Logger()
                        fun loggers() {
                            log.trace("trace {}", "trace")
                            log.debug("debug {}", "debug")
                            log.info("info {}", "info")
                            log.warn("warn {}", "warn")
                            log.error("error {}", "error")
                        }
                    }
                """.trimIndent()

        ApplicationManager.getApplication().runReadAction {
            val sourceFile = myFixture.createFile("TestLogback.kt", code).toPsiFile(project)
            assertNotNull(sourceFile)

            val ktFile = sourceFile as KtFile
            assertEquals(1, ktFile.classes.size)
            assertEquals(3, ktFile.classes[0].methods.size)

            JVMLanguageProvider().setup(project.apply { UserData.vertx(this, Vertx.vertx()) })
            SourceFileMarker.SUPPORTED_FILE_TYPES.add(KtFile::class.java)
            val fileMarker = SourceMarker.getSourceFileMarker(sourceFile)
            assertNotNull(fileMarker)

            //todo: shouldn't need toUElement()
            val result = JVMLoggerDetector(project)
                .determineLoggerStatements(
                    ktFile.classes[0].methods[1].toUElement()!!.sourcePsi as PsiNameIdentifierOwner,
                    fileMarker!!
                )
                .map { it.logPattern }
            assertEquals(5, result.size)
            assertContainsOrdered(result, "trace {}", "debug {}", "info {}", "warn {}", "error {}")
        }
    }

    fun testGroovyLogbackLogger() {
        @Language("Groovy") val code = """
                    import ch.qos.logback.classic.Logger
                    class TestLogback {
                        var log = new Logger()
                        void loggers() {
                            log.trace("trace {}", "trace")
                            log.debug("debug {}", "debug")
                            log.info("info {}", "info")
                            log.warn("warn {}", "warn")
                            log.error("error {}", "error")
                        }
                    }
                """.trimIndent()

        ApplicationManager.getApplication().runReadAction {
            val sourceFile = myFixture.createFile("TestLogback.groovy", code).toPsiFile(project)
            assertNotNull(sourceFile)

            val groovyFile = sourceFile as GroovyFile
            assertEquals(1, groovyFile.classes.size)
            assertEquals(3, groovyFile.classes[0].methods.size)

            JVMLanguageProvider().setup(project.apply { UserData.vertx(this, Vertx.vertx()) })
            SourceFileMarker.SUPPORTED_FILE_TYPES.add(GroovyFile::class.java)
            val fileMarker = SourceMarker.getSourceFileMarker(sourceFile)
            assertNotNull(fileMarker)

            val result = JVMLoggerDetector(project)
                .determineLoggerStatements(groovyFile.classes[0].methods[0], fileMarker!!)
                .map { it.logPattern }
            assertEquals(5, result.size)
            assertContainsOrdered(result, "trace {}", "debug {}", "info {}", "warn {}", "error {}")
        }
    }
}
