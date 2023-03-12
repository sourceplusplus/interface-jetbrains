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
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.service.*
import spp.protocol.insight.InsightType

@TestDataPath("\$CONTENT_ROOT/testData/")
class CountingLoopPassTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()

        JVMLanguageProvider().setup(project)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

    @Test
    fun testCountingLoop() {
//        doTest("kotlin", "kt")
        doTest("java", "java")
    }

    private fun doTest(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/CountingLoop.$extension")

        val countingLoop1 = psi.getFunctions().first { it.name == "countingLoop" }
        val loop1Paths = ProceduralAnalyzer().analyze(countingLoop1.toArtifact()!!)
        assertEquals(1, loop1Paths.size)
        val loop1Path = loop1Paths.first()
        val loop1PathInsights = loop1Path.getInsights()
        assertEquals(1, loop1PathInsights.size)
        assertEquals(InsightType.PATH_DURATION, loop1PathInsights.first().type)
        assertEquals(1000L, loop1PathInsights.first().value)
    }
}
