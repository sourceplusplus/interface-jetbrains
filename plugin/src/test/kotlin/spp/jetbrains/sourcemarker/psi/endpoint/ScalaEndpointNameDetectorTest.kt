package spp.jetbrains.sourcemarker.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import spp.jetbrains.marker.jvm.psi.EndpointDetector
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.jupiter.api.Test

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
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
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
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVCEndpoint GetMapping method_path`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(path = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVCEndpoint GetMapping method_value`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(value = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.scala", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
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
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("doGet", result.get().name)
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
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("TestController.doGet", result.get().name)
            }
        }
    }
}
