package com.sourceplusplus.sourcemarker.psi

import com.intellij.openapi.application.ApplicationManager
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.Ignore
import org.junit.Test

@Ignore
class ScalaEndpointNameDetectorTest : EndpointDetectorTest() {

    @Test
    fun `SpringMVCEndpoint RequestMapping method`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @RequestMapping(value = Array("/doGet"), method = Array(RequestMethod.GET))
                        def getStr() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get())
            }
        }
    }

    @Test
    fun `SpringMVCEndpoint GetMapping method`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(name = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get())
            }
        }
    }

    @Test
    fun `SkyWalking Trace with operation name`() {
        @Language("Scala") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace(operationName = "doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("doGet", result.get())
            }
        }
    }

    @Test
    fun `SkyWalking Trace no operation name`() {
        @Language("Scala") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("TestController.doGet", result.get())
            }
        }
    }
}
