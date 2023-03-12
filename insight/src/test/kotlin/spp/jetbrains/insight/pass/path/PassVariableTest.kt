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
package spp.jetbrains.insight.pass.path

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import spp.jetbrains.artifact.service.getCalls
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*
import spp.protocol.insight.InsightType
import spp.protocol.insight.InsightValue

@TestDataPath("\$CONTENT_ROOT/testData/")
class PassVariableTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()

        JVMLanguageProvider().setup(project)
        JavascriptLanguageProvider().setup(project)
        PythonLanguageProvider().setup(project)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

    @Test
    fun testLiteralPass() {
        doLiteralPass("kotlin", "kt")
        doLiteralPass("java", "java")
        doLiteralPass("javascript", "js")
        doLiteralPass("python", "py")
    }

    private fun doLiteralPass(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/PassVariable.$extension")

        //setup
        psi.getCalls().filter { it.text.contains("false", true) }.forEach {
            it.putUserData(
                InsightKeys.FUNCTION_DURATION.asPsiKey(),
                InsightValue.of(InsightType.FUNCTION_DURATION, 200L)
            )
        }

        val paths = ProceduralAnalyzer().analyze(
            psi.getFunctions().find { it.name!!.contains("literalPass") }.toArtifact()!!
        )
        assertEquals(1, paths.size)

        val path = paths.first()
        val pathInsights = path.getInsights()
        assertEquals(1, pathInsights.size)
        assertEquals(InsightType.PATH_DURATION, pathInsights.first().type)
        assertEquals(200L, pathInsights.first().value)
    }

    @Test
    fun testLiteralPass2() {
        doLiteralPass2("kotlin", "kt")
        doLiteralPass2("java", "java")
        doLiteralPass2("javascript", "js")
        doLiteralPass2("python", "py")
    }

    private fun doLiteralPass2(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/PassVariable.$extension")

        //setup
        psi.getCalls().filter { it.text.contains("false", true) }.forEach {
            it.putUserData(
                InsightKeys.FUNCTION_DURATION.asPsiKey(),
                InsightValue.of(InsightType.FUNCTION_DURATION, 200L)
            )
        }

        val paths = ProceduralAnalyzer().analyze(
            psi.getFunctions().find { it.name!!.contains("literalPass2") }.toArtifact()!!
        )
        assertEquals(1, paths.size)

        val path = paths.first()
        val pathInsights = path.getInsights()
        assertEquals(1, pathInsights.size)
        assertEquals(InsightType.PATH_DURATION, pathInsights.first().type)
        assertEquals(200L, pathInsights.first().value)
    }

    @Test
    fun testLiteralPass3() {
        doLiteralPass3("kotlin", "kt")
        doLiteralPass3("java", "java")
        doLiteralPass3("javascript", "js")
        doLiteralPass3("python", "py")
    }

    private fun doLiteralPass3(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/PassVariable.$extension")

        //setup
        psi.getCalls().filter { it.text.contains("false", true) }.forEach {
            it.putUserData(
                InsightKeys.FUNCTION_DURATION.asPsiKey(),
                InsightValue.of(InsightType.FUNCTION_DURATION, 200L)
            )
        }

        val paths = ProceduralAnalyzer().analyze(
            psi.getFunctions().find { it.name!!.contains("literalPass3") }.toArtifact()!!
        )
        assertEquals(1, paths.size)

        val path = paths.first()
        val pathInsights = path.getInsights()
        assertEquals(1, pathInsights.size)
        assertEquals(InsightType.PATH_DURATION, pathInsights.first().type)
        assertEquals(200L, pathInsights.first().value)
    }

    @Test
    fun testLiteralPass4() {
        doLiteralPass4("kotlin", "kt")
        doLiteralPass4("java", "java")
        doLiteralPass4("javascript", "js")
        doLiteralPass4("python", "py")
    }

    private fun doLiteralPass4(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/PassVariable.$extension")

        //setup
        psi.getCalls().filter { it.text.contains("false", true) }.forEach {
            it.putUserData(
                InsightKeys.FUNCTION_DURATION.asPsiKey(),
                InsightValue.of(InsightType.FUNCTION_DURATION, 200L)
            )
        }

        //pre-analyze
        val preAnalyzePaths = ProceduralAnalyzer().analyze(
            psi.getFunctions().find { it.name!!.contains("doSleep4") }.toArtifact()!!
        )
        assertEquals(2, preAnalyzePaths.size)

        preAnalyzePaths.forEach {
            val preAnalyzePathInsights = it.getInsights()
            assertEquals(1, preAnalyzePathInsights.size)
            assertEquals(InsightType.PATH_DURATION, preAnalyzePathInsights.first().type)
            assertEquals(200L, preAnalyzePathInsights.first().value)
        }

        val paths = ProceduralAnalyzer().analyze(
            psi.getFunctions().find { it.name!!.contains("literalPass4") }.toArtifact()!!
        )
        assertEquals(1, paths.size)

        val path = paths.first()
        val pathInsights = path.getInsights()
        assertEquals(1, pathInsights.size)
        assertEquals(InsightType.PATH_DURATION, pathInsights.first().type)
        assertEquals(200L, pathInsights.first().value)
    }
}
