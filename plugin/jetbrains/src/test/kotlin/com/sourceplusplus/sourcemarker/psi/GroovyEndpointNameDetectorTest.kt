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

class GroovyEndpointNameDetectorTest : LightPlatformCodeInsightFixture4TestCase() {

    @Test
    fun `SpringMVC RequestMapping method`() {
        @Language("Groovy") val code = """
                    class TestController {
                        @org.springframework.web.bind.annotation.RequestMapping(value = "/doGet", method = RequestMethod.GET)
                        void doGet() {}
                    }
                """.trimIndent()

        val uFile = PsiFileFactory.getInstance(project).createFileFromText(
            "TestController.groovy", JavaFileType.INSTANCE, code
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
        @Language("Groovy") val code = """
                    class TestController {
                        @org.springframework.web.bind.annotation.GetMapping(name = "/doGet")
                        void doGet() {}
                    }
                """.trimIndent()

        val uFile = PsiFileFactory.getInstance(project).createFileFromText(
            "TestController.groovy", JavaFileType.INSTANCE, code
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
        @Language("Groovy") val code = """
                    class TestController {
                        @org.apache.skywalking.apm.toolkit.trace.Trace(operationName = "doGet")
                        void doGet() {}
                    }
                """.trimIndent()

        val uFile = PsiFileFactory.getInstance(project).createFileFromText(
            "TestController.groovy", JavaFileType.INSTANCE, code
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
        @Language("Groovy") val code = """
                    class TestController {
                        @org.apache.skywalking.apm.toolkit.trace.Trace
                        void doGet() {}
                    }
                """.trimIndent()

        val uFile = PsiFileFactory.getInstance(project).createFileFromText(
            "TestController.groovy", JavaFileType.INSTANCE, code
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