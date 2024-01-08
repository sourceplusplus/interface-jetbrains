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
package spp.jetbrains.insight

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import spp.jetbrains.artifact.service.getCalls
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*

@TestDataPath("\$CONTENT_ROOT/testData/")
class BranchProbabilityTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()

        JVMLanguageProvider().setup(project)
        JavascriptLanguageProvider().setup(project)
        PythonLanguageProvider().setup(project)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

    fun testOneFourthProbability() {
        doOneFourthProbability("kotlin", "kt")
        doOneFourthProbability("java", "java")
        doOneFourthProbability("javascript", "js")
        doOneFourthProbability("python", "py")
    }

    private fun doOneFourthProbability(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/BranchProbability.$extension")

        val callExpression = psi.getCalls().find { it.text.contains("true", true) }!!
        val paths = ProceduralAnalyzer().analyzeUp(callExpression.toArtifact()!!)
        assertEquals(1, paths.size)

        val path = paths.first()
        assertEquals(4, path.descendants.size)
        assertEquals(1.0, path.descendants[0].getData(InsightKeys.PATH_EXECUTION_PROBABILITY)?.value)
        assertEquals(0.5, path.descendants[0].getData(InsightKeys.CONTROL_STRUCTURE_PROBABILITY)?.value)
        assertEquals(0.5, path.descendants[1].getData(InsightKeys.PATH_EXECUTION_PROBABILITY)?.value)
        assertEquals(0.5, path.descendants[1].getData(InsightKeys.CONTROL_STRUCTURE_PROBABILITY)?.value)
        assertEquals(0.25, path.descendants[2].getData(InsightKeys.PATH_EXECUTION_PROBABILITY)?.value)
    }
}
