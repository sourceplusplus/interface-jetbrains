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
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.artifact.service.toArtifact
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
        val oneCallMultiPath =
            ProceduralAnalyzer().analyze(psi.getFunctions().first { it.name == "oneCall" }.toArtifact()!!)
        val twoCallsMultiPath =
            ProceduralAnalyzer().analyze(psi.getFunctions().first { it.name == "twoCalls" }.toArtifact()!!)

        //assert
        assertEquals(1, oneCallMultiPath.size)
        assertEquals(1, oneCallMultiPath.first().artifacts.size)
        assertEquals(1, oneCallMultiPath.first().getInsights().size)
        assertEquals(500L, oneCallMultiPath.first().getInsights()[0].value)

        assertEquals(1, twoCallsMultiPath.size)
        assertEquals(2, twoCallsMultiPath.first().artifacts.size)
        assertEquals(1, twoCallsMultiPath.first().getInsights().size)
        assertEquals(1000L, twoCallsMultiPath.first().getInsights()[0].value)
    }
}
