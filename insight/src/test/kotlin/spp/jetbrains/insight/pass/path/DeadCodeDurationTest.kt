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
package spp.jetbrains.insight.pass.path

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*
import spp.protocol.insight.InsightType

@TestDataPath("\$CONTENT_ROOT/testData/")
class DeadCodeDurationTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()

        JVMLanguageProvider().setup(project)
        JavascriptLanguageProvider().setup(project)
        PythonLanguageProvider().setup(project)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

    fun testCode1() {
        doCode1Test("kotlin", "kt")
        doCode1Test("java", "java")
        doCode1Test("javascript", "js")
        doCode1Test("python", "py")
    }

    private fun doCode1Test(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/DeadCodeDuration.$extension")

        val paths = ProceduralAnalyzer().analyze(
            psi.getFunctions().find { it.name!!.contains("code1") }.toArtifact()!!
        )
        assertEquals(1, paths.size)

        val path = paths.first()
        val pathInsights = path.getInsights()
        assertEquals(1, pathInsights.size)
        assertEquals(InsightType.PATH_DURATION, pathInsights.first().type)
        assertEquals(200L, pathInsights.first().value)
    }

    fun testCode2() {
        doCode2Test("kotlin", "kt")
        doCode2Test("java", "java")
        doCode2Test("javascript", "js")
        doCode2Test("python", "py")
    }

    private fun doCode2Test(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/DeadCodeDuration.$extension")

        val paths = ProceduralAnalyzer().analyze(
            psi.getFunctions().find { it.name!!.contains("code2") }.toArtifact()!!
        )
        assertEquals(1, paths.size)

        val path = paths.first()
        val pathInsights = path.getInsights()
        assertEquals(1, pathInsights.size)
        assertEquals(InsightType.PATH_DURATION, pathInsights.first().type)
        assertEquals(600L, pathInsights.first().value)
    }
}
