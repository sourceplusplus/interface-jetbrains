/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
