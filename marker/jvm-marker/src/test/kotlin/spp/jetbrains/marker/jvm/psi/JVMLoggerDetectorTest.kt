/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.marker.jvm.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import io.vertx.core.Vertx
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import spp.jetbrains.UserData
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.JVMMarker
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

                    File("/opt/hostedtoolcache/jdk/11.0.10/x64").exists() -> {
                        JavaSdk.getInstance().createJdk(
                            "jdk-11", "/opt/hostedtoolcache/jdk/11.0.10/x64", false
                        )
                    }

                    File("/opt/hostedtoolcache/Java_Zulu_jdk/11.0.16-8/x64").exists() -> {
                        JavaSdk.getInstance().createJdk(
                            "jdk-11", "/opt/hostedtoolcache/Java_Zulu_jdk/11.0.16-8/x64", false
                        )
                    }

                    File("/opt/java/openjdk").exists() -> {
                        JavaSdk.getInstance().createJdk(
                            "jdk-11", "/opt/java/openjdk", false
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

            val uFile = sourceFile.toUElement() as UFile
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            JVMMarker.setup()
            SourceFileMarker.SUPPORTED_FILE_TYPES.add(PsiJavaFile::class.java)
            val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(sourceFile!!)
            assertNotNull(fileMarker)

            val result = JVMLoggerDetector(project.apply { UserData.vertx(this, Vertx.vertx()) })
                .determineLoggerStatements(uFile.classes[0].methods[0], fileMarker!!)
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

            val uFile = sourceFile.toUElement() as UFile
            assertEquals(1, uFile.classes.size)
            assertEquals(3, uFile.classes[0].methods.size)

            JVMMarker.setup()
            SourceFileMarker.SUPPORTED_FILE_TYPES.add(KtFile::class.java)
            val fileMarker = SourceMarker.getInstance(project).getSourceFileMarker(sourceFile!!)
            assertNotNull(fileMarker)

            val result = JVMLoggerDetector(project.apply { UserData.vertx(this, Vertx.vertx()) })
                .determineLoggerStatements(uFile.classes[0].methods[1], fileMarker!!)
                .map { it.logPattern }
            assertEquals(5, result.size)
            assertContainsOrdered(result, "trace {}", "debug {}", "info {}", "warn {}", "error {}")
        }
    }
}
