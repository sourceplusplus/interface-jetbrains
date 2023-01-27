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
import spp.jetbrains.artifact.model.CallArtifact
import spp.jetbrains.artifact.model.IfArtifact
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.insight.InsightPassProvider
import spp.jetbrains.insight.ProceduralAnalyzer
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
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
    fun testSimplifyBranch() {
        doSimplifyBranch("kotlin", "kt")
        doSimplifyBranch("java", "java")
        doSimplifyBranch("javascript", "js")
        doSimplifyBranch("python", "py")
    }

    private fun doSimplifyBranch(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/SimplifyBranch.$extension")

        val unsimplifiedPaths = ProceduralAnalyzer().apply {
            passProvider = InsightPassProvider().apply {
                passProvider = InsightPassProvider.FULL_NO_SIMPLIFY
            }
        }.analyze(psi.getFunctions().first { it.name == "simplifyBranch" }.toArtifact()!!)
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

        val simplifiedPaths = ProceduralAnalyzer().analyze(
            psi.getFunctions().first { it.name == "simplifyBranch" }.toArtifact()!!
        )
        assertEquals(1, simplifiedPaths.size)
        assertEquals(2, simplifiedPaths.first().artifacts.size)
        assertTrue(simplifiedPaths.first().artifacts.toList()[0] is IfArtifact)
        assertTrue(simplifiedPaths.first().artifacts.toList()[1] is CallArtifact)
        val ifChildren = (simplifiedPaths.first().artifacts.toList()[0] as IfArtifact).childArtifacts
        assertEquals(1, ifChildren.size)
        assertTrue(ifChildren[0] is CallArtifact)
    }

    @Test
    fun testSimplifyBranch2() {
        doSimplifyBranch2("kotlin", "kt")
        doSimplifyBranch2("java", "java")
        doSimplifyBranch2("javascript", "js")
        doSimplifyBranch2("python", "py")
    }

    private fun doSimplifyBranch2(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/SimplifyBranch.$extension")

        val unsimplifiedPaths = ProceduralAnalyzer().apply {
            passProvider = InsightPassProvider().apply {
                passProvider = InsightPassProvider.FULL_NO_SIMPLIFY
            }
        }.analyze(psi.getFunctions().first { it.name == "simplifyBranch2" }.toArtifact()!!)
        assertEquals(2, unsimplifiedPaths.size)
        val truePath = unsimplifiedPaths.find { it.conditions.first().first }!!
        assertEquals(3, truePath.artifacts.size)
        assertTrue(truePath.artifacts.toList()[0] is CallArtifact)
        assertTrue(truePath.artifacts.toList()[1] is IfArtifact)
        assertTrue(truePath.artifacts.toList()[2] is CallArtifact)
        val trueIfChildren = (truePath.artifacts.toList()[1] as IfArtifact).childArtifacts
        assertEquals(1, trueIfChildren.size)
        assertTrue(trueIfChildren[0] is CallArtifact)
        val falsePath = unsimplifiedPaths.find { !it.conditions.first().first }!!
        assertEquals(3, falsePath.artifacts.size)
        assertTrue(falsePath.artifacts.toList()[0] is CallArtifact)
        assertTrue(falsePath.artifacts.toList()[1] is IfArtifact)
        assertTrue(falsePath.artifacts.toList()[2] is CallArtifact)
        val falseIfChildren = (falsePath.artifacts.toList()[1] as IfArtifact).childArtifacts
        assertEquals(0, falseIfChildren.size)

        val simplifiedPaths = ProceduralAnalyzer().analyze(
            psi.getFunctions().first { it.name == "simplifyBranch2" }.toArtifact()!!
        )
        assertEquals(1, simplifiedPaths.size)
        assertEquals(3, simplifiedPaths.first().artifacts.size)
        assertTrue(simplifiedPaths.first().artifacts.toList()[0] is CallArtifact)
        assertTrue(simplifiedPaths.first().artifacts.toList()[1] is IfArtifact)
        assertTrue(simplifiedPaths.first().artifacts.toList()[2] is CallArtifact)
        val ifChildren = (simplifiedPaths.first().artifacts.toList()[1] as IfArtifact).childArtifacts
        assertEquals(1, ifChildren.size)
        assertTrue(ifChildren[0] is CallArtifact)
    }
}
