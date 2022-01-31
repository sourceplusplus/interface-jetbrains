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

class GroovyEndpointDetectorTest : EndpointDetectorTest() {

    @Test
    fun `SpringMVC RequestMapping method`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.*
                    class TestController {
                        @RequestMapping(value = "/doGet", method = RequestMethod.GET)
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

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
    fun `SpringMVC RequestMapping method with static import`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import static org.springframework.web.bind.annotation.RequestMethod.*
                    class TestController {
                        @RequestMapping(method = GET, value = "/doGet")
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

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
    fun `SpringMVC RequestMapping method with no value`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import static org.springframework.web.bind.annotation.RequestMethod.*
                    class TestController {
                        @RequestMapping(method = GET)
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

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
    fun `SpringMVC RequestMapping method with class request mapping`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import static org.springframework.web.bind.annotation.RequestMethod.*
                    @RequestMapping("/todos")
                    class TestController {
                        @RequestMapping(method = GET)
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/todos", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with class request mapping 2`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import static org.springframework.web.bind.annotation.RequestMethod.*
                    @RequestMapping("/todos/")
                    class TestController {
                        @RequestMapping(method = GET)
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/todos", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with class request mapping 3`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping
                    import static org.springframework.web.bind.annotation.RequestMethod.*
                    @RequestMapping("/todos")
                    class TestController {
                        @RequestMapping(method = GET, value = "/doGet")
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector(SourceMarkerPlugin.vertx)
                    .determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("GET:/todos/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC GetMapping method`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.GetMapping
                    class TestController {
                        @GetMapping(name = "/doGet")
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

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
    fun `SpringMVC GetMapping method_path`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.GetMapping
                    class TestController {
                        @GetMapping(path = "/doGet")
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

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
    fun `SpringMVC GetMapping method_value`() {
        @Language("Groovy") val code = """
                    import org.springframework.web.bind.annotation.GetMapping
                    class TestController {
                        @GetMapping(value = "/doGet")
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

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
        @Language("Groovy") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace(operationName = "doGet")
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

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
        @Language("Groovy") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace
                        void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.groovy", code).toUElement() as UFile

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
