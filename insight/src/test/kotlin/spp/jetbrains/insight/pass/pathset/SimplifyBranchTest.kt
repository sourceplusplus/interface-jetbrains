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
package spp.jetbrains.insight.pass.pathset

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import spp.jetbrains.insight.InsightPassProvider
import spp.jetbrains.insight.RuntimePathAnalyzer
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.model.CallArtifact
import spp.jetbrains.marker.model.IfArtifact
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*

@TestDataPath("\$CONTENT_ROOT/testData/")
class SimplifyBranchTest : BasePlatformTestCase() {

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
    fun testBranchSimplify() {
        doBranchSimplify("kotlin", "kt")
        doBranchSimplify("java", "java")
        doBranchSimplify("javascript", "js")
        doBranchSimplify("python", "py")
    }

    private fun doBranchSimplify(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/SimplifyBranch.$extension")

        val unsimplifiedPaths = RuntimePathAnalyzer().apply {
            passProvider = InsightPassProvider().apply {
                passProvider = InsightPassProvider.FULL_NO_SIMPLIFY
            }
        }.analyze(psi.getFunctions().first().toArtifact()!!)
        assertEquals(2, unsimplifiedPaths.size)
        val truePath = unsimplifiedPaths.find { it.conditions.first().first }!!
        assertEquals(2, truePath.artifacts.size)
        assertTrue(truePath.artifacts.toList()[0] is IfArtifact)
        assertTrue(truePath.artifacts.toList()[1] is CallArtifact)
        val trueIfChildren = (truePath.artifacts.toList()[0] as IfArtifact).childArtifacts
        assertEquals(1, trueIfChildren.size)
        assertTrue(trueIfChildren[0] is CallArtifact)
        val falsePath = unsimplifiedPaths.find { !it.conditions.first().first }!!
        assertEquals(2, falsePath.artifacts.size)
        assertTrue(falsePath.artifacts.toList()[0] is IfArtifact)
        assertTrue(falsePath.artifacts.toList()[1] is CallArtifact)
        val falseIfChildren = (falsePath.artifacts.toList()[0] as IfArtifact).childArtifacts
        assertEquals(0, falseIfChildren.size)

        val simplifiedPaths = RuntimePathAnalyzer().analyze(psi.getFunctions().first().toArtifact()!!)
        assertEquals(1, simplifiedPaths.size)
        assertEquals(2, simplifiedPaths.first().artifacts.size)
        assertTrue(simplifiedPaths.first().artifacts.toList()[0] is IfArtifact)
        assertTrue(simplifiedPaths.first().artifacts.toList()[1] is CallArtifact)
        val ifChildren = (simplifiedPaths.first().artifacts.toList()[0] as IfArtifact).childArtifacts
        assertEquals(1, ifChildren.size)
        assertTrue(ifChildren[0] is CallArtifact)
    }
}
