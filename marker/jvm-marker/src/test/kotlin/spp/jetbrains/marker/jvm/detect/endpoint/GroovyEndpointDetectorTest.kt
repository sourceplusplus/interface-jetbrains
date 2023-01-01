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

class GroovyEndpointDetectorTest : AbstractEndpointDetectorTest() {

    fun `test SpringMVC RequestMapping method`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with static import`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with no value`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping 2`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping 3`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method_path`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method_value`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SkyWalking Trace with operation name`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("doGet", result.first().name)
            }
        }
    }

    fun `test SkyWalking Trace no operation name`() {
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

            safeRunBlocking {
                val result = JVMEndpointDetector(project).determineEndpointName(uFile.classes[0].methods[0]).await()
                assertEquals(1, result.size)
                assertEquals("TestController.doGet", result.first().name)
            }
        }
    }
}
