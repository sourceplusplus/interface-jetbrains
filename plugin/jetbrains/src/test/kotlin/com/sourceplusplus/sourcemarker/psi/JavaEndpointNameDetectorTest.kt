package com.sourceplusplus.sourcemarker.psi

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.Test

class JavaEndpointNameDetectorTest : LightPlatformCodeInsightFixture4TestCase() {

    @Test
    fun `SpringMVC RequestMapping method`() {
        @Language("Java") val code = """
                    public class TestController {
                        @org.springframework.web.bind.annotation.RequestMapping(value = "/doGet", method = RequestMethod.GET)
                        public void doGet() {}
                    }
                """.trimIndent()

        val uFile = PsiFileFactory.getInstance(project).createFileFromText(
            "TestController.java", JavaFileType.INSTANCE, code
        ).toUElement() as UFile
        assertEquals(1, uFile.classes.size)
        assertEquals(1, uFile.classes[0].methods.size)

        runBlocking {
            val result = EndpointNameDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
            assertTrue(result.isPresent)
            assertEquals("{GET}/doGet", result.get())
        }
    }

    @Test
    fun `SpringMVC GetMapping method`() {
        @Language("Java") val code = """
                    public class TestController {
                        @org.springframework.web.bind.annotation.GetMapping(name = "/doGet")
                        public void doGet() {}
                    }
                """.trimIndent()

        val uFile = PsiFileFactory.getInstance(project).createFileFromText(
            "TestController.java", JavaFileType.INSTANCE, code
        ).toUElement() as UFile
        assertEquals(1, uFile.classes.size)
        assertEquals(1, uFile.classes[0].methods.size)

        runBlocking {
            val result = EndpointNameDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
            assertTrue(result.isPresent)
            assertEquals("{GET}/doGet", result.get())
        }
    }

    @Test
    fun `SkyWalking Trace with operation name`() {
        @Language("Java") val code = """
                    public class TestController {
                        @org.apache.skywalking.apm.toolkit.trace.Trace(operationName = "doGet")
                        public void doGet() {}
                    }
                """.trimIndent()

        val uFile = PsiFileFactory.getInstance(project).createFileFromText(
            "TestController.java", JavaFileType.INSTANCE, code
        ).toUElement() as UFile
        assertEquals(1, uFile.classes.size)
        assertEquals(1, uFile.classes[0].methods.size)

        runBlocking {
            val result = EndpointNameDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
            assertTrue(result.isPresent)
            assertEquals("doGet", result.get())
        }
    }

    @Test
    fun `SkyWalking Trace no operation name`() {
        @Language("Java") val code = """
                    public class TestController {
                        @org.apache.skywalking.apm.toolkit.trace.Trace
                        public void doGet() {}
                    }
                """.trimIndent()

        val uFile = PsiFileFactory.getInstance(project).createFileFromText(
            "TestController.java", JavaFileType.INSTANCE, code
        ).toUElement() as UFile
        assertEquals(1, uFile.classes.size)
        assertEquals(1, uFile.classes[0].methods.size)

        runBlocking {
            val result = EndpointNameDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
            assertTrue(result.isPresent)
            assertEquals("TestController.doGet", result.get())
        }
    }
}