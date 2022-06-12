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
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.jupiter.api.Test
import spp.jetbrains.marker.jvm.JVMEndpointDetector
import spp.jetbrains.sourcemarker.SourceMarkerPlugin

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
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with static import`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import org.springframework.web.bind.annotation.RequestMethod.*
                    class TestController {
                        @RequestMapping(method = [GET], value = ["/doGet"])
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with no value`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import org.springframework.web.bind.annotation.RequestMethod.*
                    class TestController {
                        @RequestMapping(method = [GET])
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with class request mapping`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import org.springframework.web.bind.annotation.RequestMethod.*
                    @RequestMapping("/todos")
                    class TestController {
                        @RequestMapping(method = [GET])
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/todos", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with class request mapping 2`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import org.springframework.web.bind.annotation.RequestMethod.*
                    @RequestMapping("/todos/")
                    class TestController {
                        @RequestMapping(method = [GET])
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/todos", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with class request mapping 3`() {
        @Language("Kt") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import org.springframework.web.bind.annotation.RequestMethod.*
                    @RequestMapping("/todos")
                    class TestController {
                        @RequestMapping(method = [GET], value = ["/doGet"])
                        fun doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.kt", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(2, uFile.classes[0].methods.size)

            runBlocking {
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/todos/doGet", result.get().name)
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
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
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
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
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
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/doGet", result.get().name)
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
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/", result.get().name)
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
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
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
                val result = JVMEndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("TestController.doGet", result.get().name)
            }
        }
    }
}
