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
import org.junit.jupiter.api.Test
import spp.jetbrains.artifact.service.getChildIfs
import spp.jetbrains.artifact.service.getFunctions
import spp.jetbrains.artifact.service.toArtifact
import spp.jetbrains.marker.js.JavascriptLanguageProvider
import spp.jetbrains.marker.jvm.JVMLanguageProvider
import spp.jetbrains.marker.py.PythonLanguageProvider
import spp.jetbrains.marker.service.*

@TestDataPath("\$CONTENT_ROOT/testData/")
class LiteralBranchProbabilityTest : BasePlatformTestCase() {

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
    fun testBooleanConstant() {
        doTestBooleanConstant("kotlin", "kt")
        doTestBooleanConstant("java", "java")
        doTestBooleanConstant("javascript", "js")
        doTestBooleanConstant("python", "py")
    }

    private fun doTestBooleanConstant(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/LiteralBranchProbability.$extension")

        val method = psi.getFunctions().first { it.name == "booleanConstant" }
        val ifExpression = method.getChildIfs().first()

        val paths = ProceduralAnalyzer().apply {
            passProvider = InsightPassProvider.FULL_NO_SIMPLIFY
        }.analyzeUp(ifExpression.toArtifact()!!)
        assertEquals(2, paths.size)

        //todo: the false path is actually dead code, should add insight for that
        val truePath = paths.find { it.conditions.any { it.first } }!!
        assertEquals(3, truePath.descendants.size)
        assertTrue(truePath.descendants[0].isControlStructure())
        assertTrue(truePath.descendants[1].isCall())
        assertTrue(truePath.descendants[2].isLiteral())
        assertEquals(1.0, truePath.descendants[0].getData(InsightKeys.PATH_EXECUTION_PROBABILITY)?.value)
    }

    @Test
    fun testNumberCompare() {
        doTestNumberCompare("kotlin", "kt")
        doTestNumberCompare("java", "java")
        doTestNumberCompare("javascript", "js")
        doTestNumberCompare("python", "py")
    }

    private fun doTestNumberCompare(language: String, extension: String) {
        val psi = myFixture.configureByFile("$language/LiteralBranchProbability.$extension")

        val method = psi.getFunctions().first { it.name == "numberCompare" }
        val ifExpression = method.getChildIfs().first()

        val paths = ProceduralAnalyzer().apply {
            passProvider = InsightPassProvider.FULL_NO_SIMPLIFY
        }.analyzeUp(ifExpression.toArtifact()!!)
        assertEquals(2, paths.size)

        //todo: the false path is actually dead code, should add insight for that
        val truePath = paths.find { it.conditions.any { it.first } }!!
        assertEquals(3, truePath.descendants.size)
        assertTrue(truePath.descendants[0].isControlStructure())
        assertTrue(truePath.descendants[1].isCall())
        assertTrue(truePath.descendants[2].isLiteral())
        assertEquals(1.0, truePath.descendants[0].getData(InsightKeys.PATH_EXECUTION_PROBABILITY)?.value)
    }
}
