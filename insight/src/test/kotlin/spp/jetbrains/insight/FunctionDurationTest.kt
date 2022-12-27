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
class FunctionDurationTest : BasePlatformTestCase() {

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
        val psi = myFixture.configureByFile("$language/SequentialMethodCalls.$extension")

        //setup
        psi.getFunctions().first { it.name == "duration500ms" }.putUserData(
            InsightKeys.FUNCTION_DURATION.asPsiKey(),
            InsightValue.of(InsightType.FUNCTION_DURATION, 500L)
        )

        //calculate
        val oneCallPath =
            ProceduralAnalyzer().analyze(psi.getFunctions().first { it.name == "oneCall" }.toArtifact()!!)
        val twoCallsPath =
            ProceduralAnalyzer().analyze(psi.getFunctions().first { it.name == "twoCalls" }.toArtifact()!!)

        //assert
        assertEquals(1, oneCallPath.size)
        assertEquals(1, oneCallPath.first().artifacts.size)
        assertEquals(1, oneCallPath.first().getInsights().size)
        assertEquals(500L, oneCallPath.first().getInsights()[0].value)

        assertEquals(1, twoCallsPath.size)
        assertEquals(2, twoCallsPath.first().artifacts.size)
        assertEquals(1, twoCallsPath.first().getInsights().size)
        assertEquals(1000L, twoCallsPath.first().getInsights()[0].value)
    }
}
