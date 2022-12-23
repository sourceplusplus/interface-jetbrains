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
import spp.jetbrains.marker.SourceMarkerKeys
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
                SourceMarkerKeys.CONTROL_STRUCTURE_PROBABILITY.asPsiKey(),
                InsightValue.of(InsightType.CONTROL_STRUCTURE_PROBABILITY, 0.5)
            )
            it.putUserData(
                SourceMarkerKeys.METHOD_DURATION.asPsiKey(),
                InsightValue.of(InsightType.METHOD_DURATION, 100L)
            )
        }
        psi.getCalls().filter { it.text.contains("true", true) || it.text.contains("false", true) }.forEach {
            it.putUserData(
                SourceMarkerKeys.METHOD_DURATION.asPsiKey(),
                InsightValue.of(InsightType.METHOD_DURATION, 200L)
            )
        }
        psi.getCalls().filter { it.text.contains("true", true) || it.text.contains("false", true) }.forEach {
            it.putUserData(
                SourceMarkerKeys.METHOD_DURATION.asPsiKey(),
                InsightValue.of(InsightType.METHOD_DURATION, 200L)
            )
        }

        val paths = RuntimePathAnalyzer().analyze(psi.getFunctions().first().toArtifact()!!)
        assertEquals(4, paths.size)

        //[true, true]
        val path1 = paths.toList()[0]
        assertEquals(1, path1.getInsights().size)
        assertEquals(400L, path1.getInsights()[0].value) //InsightKeys.PATH_DURATION
        assertEquals(4, path1.descendants.size)
        assertEquals(2, path1.conditions.size)
        assertTrue(path1.conditions[0].second.condition?.text?.contains("random() > 0.5") == true)
        assertTrue(path1.conditions[0].first)
        assertTrue(path1.conditions[1].second.condition?.text?.contains("random() > 0.5") == true)
        assertTrue(path1.conditions[1].first)
        assertTrue(path1.descendants[0].isControlStructure())
        assertTrue(path1.descendants[1].isControlStructure())
        assertTrue(path1.descendants[2].isCall())
        assertTrue(path1.descendants[3].isLiteral())

        val path1CallInsights = path1.descendants[2].getInsights()
        assertEquals(2, path1CallInsights.size)
        assertEquals(200L, path1CallInsights[0].value)
        assertEquals(0.25, path1CallInsights[1].value)
//        assertEquals(InsightType.METHOD_DURATION, path1CallInsights.find { it.type }) //todo: save type to insightvalue?

        //[true, false]
        val path2 = paths.toList()[1]
        assertEquals(1, path2.getInsights().size)
        assertEquals(200L, path2.getInsights()[0].value) //InsightKeys.PATH_DURATION
        assertEquals(2, path2.descendants.size)
        assertEquals(2, path2.conditions.size)
        assertTrue(path2.conditions[0].second.condition?.text?.contains("random() > 0.5") == true)
        assertTrue(path2.conditions[0].first)
        assertTrue(path2.conditions[1].second.condition?.text?.contains("random() > 0.5") == true)
        assertFalse(path2.conditions[1].first)
        assertTrue(path2.descendants[0].isControlStructure())
        assertTrue(path2.descendants[1].isControlStructure())

        //[false, false]
        val path3 = paths.toList()[2]
        assertEquals(1, path3.getInsights().size)
        assertEquals(300L, path3.getInsights()[0].value) //InsightKeys.PATH_DURATION
        assertEquals(3, path3.descendants.size)
        assertEquals(1, path3.conditions.size)
        assertTrue(path3.conditions[0].second.condition?.text?.contains("random() > 0.5") == true)
        assertFalse(path3.conditions[0].first)
        assertTrue(path3.descendants[0].isControlStructure())
        assertTrue(path3.descendants[1].isCall())
        assertTrue(path3.descendants[2].isLiteral())

        val path3CallInsights = path3.descendants[1].getInsights()
        assertEquals(2, path3CallInsights.size)
        assertEquals(200L, path3CallInsights[0].value)
        assertEquals(0.5, path3CallInsights[1].value)
//        assertEquals(InsightType.METHOD_DURATION, path1CallInsights.find { it.type }) //todo: save type to insightvalue?

        //[false, true]
        val path4 = paths.toList()[3]
        assertEquals(1, path4.getInsights().size)
        assertEquals(300L, path4.getInsights()[0].value) //InsightKeys.PATH_DURATION
        assertEquals(3, path4.descendants.size)
        assertEquals(1, path4.conditions.size)
        assertTrue(path4.conditions[0].second.condition?.text?.contains("random() > 0.5") == true)
        assertFalse(path4.conditions[0].first)
        assertTrue(path4.descendants[0].isControlStructure())
        assertTrue(path4.descendants[1].isCall())
        assertTrue(path4.descendants[2].isLiteral())

        val path4CallInsights = path4.descendants[1].getInsights()
        assertEquals(2, path4CallInsights.size)
        assertEquals(200L, path4CallInsights[0].value)
        assertEquals(0.5, path4CallInsights[1].value)
//        assertEquals(InsightType.METHOD_DURATION, path1CallInsights.find { it.type }) //todo: save type to insightvalue?
    }
}
