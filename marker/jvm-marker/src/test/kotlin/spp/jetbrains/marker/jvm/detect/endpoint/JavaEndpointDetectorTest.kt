/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
import com.intellij.psi.PsiMethod
import io.vertx.kotlin.coroutines.await
import org.intellij.lang.annotations.Language
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.artifact.service.*
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector
import spp.jetbrains.marker.jvm.service.*
import spp.jetbrains.marker.service.*
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

class JavaEndpointDetectorTest : AbstractEndpointDetectorTest() {

    override fun setUp() {
        super.setUp()

        SourceMarkerUtils.getJvmLanguages().let {
            ArtifactNamingService.addService(JVMArtifactNamingService(), it)
            ArtifactScopeService.addService(JVMArtifactScopeService(), it)
            ArtifactModelService.addService(JVMArtifactModelService(), it)
        }
    }

    fun `test SpringMVC RequestMapping method`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.RequestMapping;
                public class TestController {
                    @RequestMapping(value = "/doGet", method = RequestMethod.GET)
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with static import`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.RequestMapping;
                import static org.springframework.web.bind.annotation.RequestMethod.*;
                public class TestController {
                    @RequestMapping(method = GET, value = "/doGet")
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with no value`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.RequestMapping;
                import static org.springframework.web.bind.annotation.RequestMethod.*;
                public class TestController {
                    @RequestMapping(method = GET)
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.RequestMapping;
                import static org.springframework.web.bind.annotation.RequestMethod.*;
                @RequestMapping("/todos")
                public class TestController {
                    @RequestMapping(method = GET)
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping 2`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.RequestMapping;
                import static org.springframework.web.bind.annotation.RequestMethod.*;
                @RequestMapping("/todos/")
                public class TestController {
                    @RequestMapping(method = GET)
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos", result.first().name)
            }
        }
    }

    fun `test SpringMVC RequestMapping method with class request mapping 3`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.RequestMapping;
                import static org.springframework.web.bind.annotation.RequestMethod.*;
                @RequestMapping("/todos")
                public class TestController {
                    @RequestMapping(method = GET, value = "/doGet")
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.GetMapping;
                public class TestController {
                    @GetMapping(name = "/doGet")
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method_value`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.GetMapping;
                public class TestController {
                    @GetMapping(value = "/doGet")
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVC GetMapping method_path`() {
        @Language("Java") val code = """
                import org.springframework.web.bind.annotation.GetMapping;
                public class TestController {
                    @GetMapping(path = "/doGet")
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SkyWalking Trace with operation name`() {
        @Language("Java") val code = """
                import org.apache.skywalking.apm.toolkit.trace.Trace;
                public class TestController {
                    @Trace(operationName = "doGet")
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("doGet", result.first().name)
            }
        }
    }

    fun `test SkyWalking Trace no operation name`() {
        @Language("Java") val code = """
                import org.apache.skywalking.apm.toolkit.trace.Trace;
                public class TestController {
                    @Trace
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("TestController.doGet", result.first().name)
            }
        }
    }

    fun `test Micronaut endpoint`() {
        @Language("Java") val code = """
                import io.micronaut.http.annotation.Controller;
                import io.micronaut.http.annotation.Get;
                @Controller("/todos")
                public class TestController {
                    @Get("/doGet")
                    public void doGet() {}
                }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.java", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as PsiMethod
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/todos/doGet", result.first().name)
            }
        }
    }
}
