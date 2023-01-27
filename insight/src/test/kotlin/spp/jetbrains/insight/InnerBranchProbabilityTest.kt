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
package spp.jetbrains.insight

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import spp.jetbrains.artifact.service.getCalls
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*

@TestDataPath("\$CONTENT_ROOT/testData/")
class InnerBranchProbabilityTest : BasePlatformTestCase() {

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
    fun testInnerStatement() {
        doTestInnerStatement("kotlin", "kt")
        doTestInnerStatement("java", "java")
        doTestInnerStatement("javascript", "js")
        doTestInnerStatement("python", "py")
    }

    private fun doTestInnerStatement(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/InnerBranchProbability.$extension")

        val callExpression1 = psi.getCalls().find { it.text.contains("true", true) }!!
        val truePath = ProceduralAnalyzer().apply {
            passProvider = InsightPassProvider.FULL_NO_SIMPLIFY
        }.analyzeUp(callExpression1.toArtifact()!!)
        assertEquals(1.0, truePath.first().artifacts.first().getData(InsightKeys.PATH_EXECUTION_PROBABILITY)?.value)

        val callExpression2 = psi.getCalls().find { it.text.contains("false", true) }!!
        val falsePath = ProceduralAnalyzer().apply {
            passProvider = InsightPassProvider.FULL_NO_SIMPLIFY
        }.analyzeUp(callExpression2.toArtifact()!!)
        assertEquals(1.0, falsePath.first().artifacts.last().getData(InsightKeys.PATH_EXECUTION_PROBABILITY)?.value)
        assertEquals(0.0, falsePath.first().artifacts.last().getData(InsightKeys.CONTROL_STRUCTURE_PROBABILITY)?.value)
    }
}
