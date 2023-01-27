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
package spp.jetbrains.insight.pass.artifact

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import spp.jetbrains.artifact.model.IfArtifact
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*

@TestDataPath("\$CONTENT_ROOT/testData/")
class RandomConditionalPassTest : BasePlatformTestCase() {

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
    fun testMathRandom() {
        doTest("kotlin", "kt")
        doTest("java", "java")
        doTest("javascript", "js")
        doTest("python", "py")
    }

    private fun doTest(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/RandomConditional.$extension")
        val paths = ProceduralAnalyzer().analyze(psi.getFunctions().first().toArtifact()!!)
        assertEquals(2, paths.size)

        val falsePath = paths.first { !it.conditions.first().first }
        val ifArtifactFalse = falsePath.artifacts.first() as IfArtifact
        assertEquals(1.0, ifArtifactFalse.getData(InsightKeys.PATH_EXECUTION_PROBABILITY)!!.value)
        assertEquals(0.74, ifArtifactFalse.getData(InsightKeys.CONTROL_STRUCTURE_PROBABILITY)!!.value)

        val truePath = paths.first { it.conditions.first().first }
        val ifArtifactTrue = truePath.artifacts.first() as IfArtifact
        assertEquals(1.0, ifArtifactTrue.getData(InsightKeys.PATH_EXECUTION_PROBABILITY)!!.value)
        assertEquals(0.26, ifArtifactTrue.getData(InsightKeys.CONTROL_STRUCTURE_PROBABILITY)!!.value)
    }
}
