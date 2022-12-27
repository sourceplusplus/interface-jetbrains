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
package spp.jetbrains.insight.pass.path

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.insight.InsightKeys
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*
import spp.protocol.insight.InsightType
import spp.protocol.insight.InsightValue

@TestDataPath("\$CONTENT_ROOT/testData/")
class ReanalyzeDurationTest : BasePlatformTestCase() {

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
    fun testReanalyzeDuration() {
        doTest("kotlin", "kt")
        doTest("java", "java")
        doTest("javascript", "js")
        doTest("python", "py")
    }

    private fun doTest(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/ReanalyzeDuration.$extension")

        //setup
        psi.getCalls().filter { it.text.contains("true", true) }.forEach {
            it.putUserData(
                InsightKeys.FUNCTION_DURATION.asPsiKey(),
                InsightValue.of(InsightType.FUNCTION_DURATION, 200L)
            )
        }

        //analyze code2
        ProceduralAnalyzer().analyze(
            psi.getFunctions().find { it.name!!.contains("code2") }.toArtifact()!!
        )

        //analyze code1
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
}
