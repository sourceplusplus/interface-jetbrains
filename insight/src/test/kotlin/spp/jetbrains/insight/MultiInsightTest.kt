/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
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
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*
import spp.protocol.insight.InsightType
import spp.protocol.insight.InsightValue

@TestDataPath("\$CONTENT_ROOT/testData/")
class MultiInsightTest : BasePlatformTestCase() {

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
    fun testSequentialMethodCalls() {
        doTest("kotlin", "kt")
        doTest("java", "java")
        doTest("javascript", "js")
        doTest("python", "py")
    }

    private fun doTest(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/MultiInsight.$extension")

        //setup
        psi.getChildIfs().forEach {
            it.putUserData(
                InsightKeys.CONTROL_STRUCTURE_PROBABILITY.asPsiKey(),
                InsightValue.of(InsightType.CONTROL_STRUCTURE_PROBABILITY, 0.5)
            )
            it.putUserData(
                InsightKeys.FUNCTION_DURATION.asPsiKey(),
                InsightValue.of(InsightType.FUNCTION_DURATION, 100L)
            )
        }
        psi.getCalls().filter { it.text.contains("true", true) || it.text.contains("false", true) }.forEach {
            it.putUserData(
                InsightKeys.FUNCTION_DURATION.asPsiKey(),
                InsightValue.of(InsightType.FUNCTION_DURATION, 200L)
            )
        }

        val paths = ProceduralAnalyzer().analyze(psi.getFunctions().first().toArtifact()!!)
        assertEquals(4, paths.size)

        //[false, false]
        val falseFalsePath = paths.toList()[0]
        assertEquals(1, falseFalsePath.getInsights().size)
        assertEquals(300L, falseFalsePath.getInsights()[0].value) //InsightKeys.PATH_DURATION
        assertEquals(3, falseFalsePath.descendants.size)
        assertEquals(1, falseFalsePath.conditions.size)
        assertTrue(falseFalsePath.conditions[0].second.condition?.text?.contains("random() > 0.5") == true)
        assertFalse(falseFalsePath.conditions[0].first)
        assertTrue(falseFalsePath.descendants[0].isControlStructure())
        assertTrue(falseFalsePath.descendants[1].isCall())
        assertTrue(falseFalsePath.descendants[2].isLiteral())

        val falseFalseCallInsights = falseFalsePath.descendants[1].getInsights()
        assertEquals(2, falseFalseCallInsights.size)
        assertEquals(200L, falseFalseCallInsights[0].value)
        assertEquals(0.5, falseFalseCallInsights[1].value)
//        assertEquals(InsightType.FUNCTION_DURATION, path1CallInsights.find { it.type }) //todo: save type to insightvalue?

        //[false, true]
        val falseTruePath = paths.toList()[1]
        assertEquals(1, falseTruePath.getInsights().size)
        assertEquals(300L, falseTruePath.getInsights()[0].value) //InsightKeys.PATH_DURATION
        assertEquals(3, falseTruePath.descendants.size)
        assertEquals(1, falseTruePath.conditions.size)
        assertTrue(falseTruePath.conditions[0].second.condition?.text?.contains("random() > 0.5") == true)
        assertFalse(falseTruePath.conditions[0].first)
        assertTrue(falseTruePath.descendants[0].isControlStructure())
        assertTrue(falseTruePath.descendants[1].isCall())
        assertTrue(falseTruePath.descendants[2].isLiteral())

        val falseTrueCallInsights = falseTruePath.descendants[1].getInsights()
        assertEquals(2, falseTrueCallInsights.size)
        assertEquals(200L, falseTrueCallInsights[0].value)
        assertEquals(0.5, falseTrueCallInsights[1].value)
//        assertEquals(InsightType.FUNCTION_DURATION, path1CallInsights.find { it.type }) //todo: save type to insightvalue?

        //[true, false]
        val trueFalsePath = paths.toList()[2]
        assertEquals(1, trueFalsePath.getInsights().size)
        assertEquals(200L, trueFalsePath.getInsights()[0].value) //InsightKeys.PATH_DURATION
        assertEquals(2, trueFalsePath.descendants.size)
        assertEquals(2, trueFalsePath.conditions.size)
        assertTrue(trueFalsePath.conditions[0].second.condition?.text?.contains("random() > 0.5") == true)
        assertTrue(trueFalsePath.conditions[0].first)
        assertTrue(trueFalsePath.conditions[1].second.condition?.text?.contains("random() > 0.5") == true)
        assertFalse(trueFalsePath.conditions[1].first)
        assertTrue(trueFalsePath.descendants[0].isControlStructure())
        assertTrue(trueFalsePath.descendants[1].isControlStructure())

        //[true, true]
        val trueTruePath = paths.toList()[3]
        assertEquals(1, trueTruePath.getInsights().size)
        assertEquals(400L, trueTruePath.getInsights()[0].value) //InsightKeys.PATH_DURATION
        assertEquals(4, trueTruePath.descendants.size)
        assertEquals(2, trueTruePath.conditions.size)
        assertTrue(trueTruePath.conditions[0].second.condition?.text?.contains("random() > 0.5") == true)
        assertTrue(trueTruePath.conditions[0].first)
        assertTrue(trueTruePath.conditions[1].second.condition?.text?.contains("random() > 0.5") == true)
        assertTrue(trueTruePath.conditions[1].first)
        assertTrue(trueTruePath.descendants[0].isControlStructure())
        assertTrue(trueTruePath.descendants[1].isControlStructure())
        assertTrue(trueTruePath.descendants[2].isCall())
        assertTrue(trueTruePath.descendants[3].isLiteral())

        val trueTrueCallInsights = trueTruePath.descendants[2].getInsights()
        assertEquals(2, trueTrueCallInsights.size)
        assertEquals(200L, trueTrueCallInsights[0].value)
        assertEquals(0.25, trueTrueCallInsights[1].value)
//        assertEquals(InsightType.FUNCTION_DURATION, path1CallInsights.find { it.type }) //todo: save type to insightvalue?
    }
}
