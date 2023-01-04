/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.jvm.detect.endpoint

import com.intellij.openapi.application.ApplicationManager
import io.vertx.kotlin.coroutines.await
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector

class KotlinEndpointDetectorTest : AbstractEndpointDetectorTest() {

    fun `test SpringMVC RequestMapping method`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with static import`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with no value`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping 2`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping 3`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method_value`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method_path`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method_name`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/", result.first().name)
            }
        }
    }

    fun `test SkyWalking Trace with operation name`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("doGet", result.first().name)
            }
        }
    }

    fun `test SkyWalking Trace no operation name`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("TestController.doGet", result.first().name)
            }
        }
    }
}
