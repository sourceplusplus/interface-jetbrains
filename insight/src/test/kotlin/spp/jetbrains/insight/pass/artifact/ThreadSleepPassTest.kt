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
package spp.jetbrains.insight.pass.artifact

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.insight.getDuration
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*

@TestDataPath("\$CONTENT_ROOT/testData/")
class ThreadSleepPassTest : BasePlatformTestCase() {

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
    fun testThreadSleep() {
        doTestThreadSleep("kotlin", "kt")
        doTestThreadSleep("java", "java")
//        doTestThreadSleep("javascript", "js")
//        doTestThreadSleep("python", "py")
    }

    private fun doTestThreadSleep(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/ThreadSleep.$extension")

        val sleepExpression = psi.getCalls().find { it.text.contains("sleep") }!!
        val path = ProceduralAnalyzer().analyzeUp(sleepExpression.toArtifact()!!).first()
        val insights = path.getInsights()
        assertEquals(1, insights.size)
        assertEquals(1000L, insights.first().value)
        assertEquals(1000L, path.artifacts.first().getDuration())
    }
}
