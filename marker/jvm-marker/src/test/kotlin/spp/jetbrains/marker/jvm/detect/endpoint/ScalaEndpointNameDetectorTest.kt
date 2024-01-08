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
import io.vertx.kotlin.coroutines.await
import org.intellij.lang.annotations.Language
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.artifact.service.ArtifactModelService
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.artifact.service.getClasses
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.jvm.detect.JVMEndpointDetector
import spp.jetbrains.marker.jvm.service.JVMArtifactModelService
import spp.jetbrains.marker.jvm.service.JVMArtifactNamingService
import spp.jetbrains.marker.jvm.service.JVMArtifactScopeService
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark

class ScalaEndpointNameDetectorTest : AbstractEndpointDetectorTest() {

    override fun setUp() {
        super.setUp()

        SourceMarkerUtils.getJvmLanguages().let {
            ArtifactNamingService.addService(JVMArtifactNamingService(), it)
            ArtifactScopeService.addService(JVMArtifactScopeService(), it)
            ArtifactModelService.addService(JVMArtifactModelService(), it)
        }
    }

    fun `test SpringMVCEndpoint RequestMapping method`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @RequestMapping(value = Array("/doGet"), method = Array(RequestMethod.GET))
                        def getStr() {}
                    }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.scala", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as ScFunctionDefinition
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVCEndpoint GetMapping method`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(name = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.scala", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as ScFunctionDefinition
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/", result.first().name)
            }
        }
    }

    fun `test SpringMVCEndpoint GetMapping method_path`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(path = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.scala", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as ScFunctionDefinition
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SpringMVCEndpoint GetMapping method_value`() {
        @Language("Scala") val code = """
                    import org.springframework.web.bind.annotation._
                    class TestController {
                        @GetMapping(value = "/doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.scala", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as ScFunctionDefinition
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("GET:/doGet", result.first().name)
            }
        }
    }

    fun `test SkyWalking Trace with operation name`() {
        @Language("Scala") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace(operationName = "doGet")
                        def doGet() {}
                    }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.scala", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as ScFunctionDefinition
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("doGet", result.first().name)
            }
        }
    }

    fun `test SkyWalking Trace no operation name`() {
        @Language("Scala") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace
                    class TestController {
                        @Trace
                        def doGet() {}
                    }
                """.trimIndent()
        val psiFile = myFixture.configureByText("TestController.scala", code)

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, psiFile.getClasses().size)
            assertEquals(1, psiFile.getClasses()[0].getFunctions().size)

            safeRunBlocking {
                val guideMark = MethodGuideMark(
                    SourceFileMarker(psiFile),
                    psiFile.getClasses()[0].getFunctions()[0] as ScFunctionDefinition
                )
                val result = JVMEndpointDetector(project).determineEndpointName(guideMark).await()
                assertEquals(1, result.size)
                assertEquals("TestController.doGet", result.first().name)
            }
        }
    }
}
