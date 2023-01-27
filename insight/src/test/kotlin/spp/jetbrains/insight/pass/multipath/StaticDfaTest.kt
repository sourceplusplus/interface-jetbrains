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
package spp.jetbrains.insight.pass.multipath

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
import spp.jetbrains.marker.service.*

@TestDataPath("\$CONTENT_ROOT/testData/")
class StaticDfaTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()

        JVMLanguageProvider().setup(project)
        JavascriptLanguageProvider().setup(project)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/"
    }

    @Test
    fun testStaticDfa() {
        doTest("kotlin", "kt")
        doTest("java", "java")
    }

    private fun doTest(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/StaticDfa.$extension")
        val paths = ProceduralAnalyzer().analyze(psi.getFunctions().first().toArtifact()!!)
        assertEquals(1, paths.size)

        val ifArtifact = paths.first().artifacts.first() as IfArtifact
        assertEquals(1.0, ifArtifact.getData(InsightKeys.CONTROL_STRUCTURE_PROBABILITY)!!.value)
    }
}
