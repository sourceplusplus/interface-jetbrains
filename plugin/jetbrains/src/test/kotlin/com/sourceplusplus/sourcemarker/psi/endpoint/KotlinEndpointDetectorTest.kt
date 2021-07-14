package com.sourceplusplus.sourcemarker.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.sourcemarker.psi.EndpointDetector
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.jupiter.api.Test

class KotlinEndpointDetectorTest : EndpointDetectorTest() {

    @Test
    fun `SpringMVC RequestMapping method`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    class TestController {
                        @RequestMapping(value = ["/doGet"], method = [RequestMethod.GET])
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC GetMapping method`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.GetMapping
                    class TestController {
                        @GetMapping("/doGet")
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC GetMapping method_value`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.GetMapping
                    class TestController {
                        @GetMapping(value = "/doGet")
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC GetMapping method_path`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.GetMapping
                    class TestController {
                        @GetMapping(path = "/doGet")
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC GetMapping method_name`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.GetMapping
                    class TestController {
                        @GetMapping(name = "/doGet")
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/", result.get().name)
            }
        }
    }

    @Test
    fun `SkyWalking Trace with operation name`() {
        @Language("Kt") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace(operationName = "doGet")
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SkyWalking Trace no operation name`() {
        @Language("Kt") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("TestController.doGet", result.get().name)
            }
        }
    }
}
