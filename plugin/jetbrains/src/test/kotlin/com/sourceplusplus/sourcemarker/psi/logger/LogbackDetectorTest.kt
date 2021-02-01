package com.sourceplusplus.sourcemarker.psi.logger

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.sourceplusplus.sourcemarker.psi.LoggerDetector
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LogbackDetectorTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : DefaultLightProjectDescriptor() {
            override fun getSdk(): Sdk = JavaSdk.getInstance().createJdk(
                "jdk-1.8", "/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre"
            )

            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)
                model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel = LanguageLevel.JDK_1_8
            }
        }
    }

    @Before
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

    @After
    public override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `logbackTest`() {
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
            val sourceFile = PsiFileFactory.getInstance(project).createFileFromText(
                "TestLogback.java", JavaFileType.INSTANCE, code
            ) as PsiJavaFile
            val uFile = sourceFile.toUElement() as UFile
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = LoggerDetector().getOrFindLoggerStatements(uFile.classes[0].methods[0]).await()
                assertEquals(5, result.size)
                assertContainsOrdered(result, "trace {}", "debug {}", "info {}", "warn {}", "error {}")
            }
        }
    }
}
