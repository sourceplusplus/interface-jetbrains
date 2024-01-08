/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.marker.jvm

import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import org.intellij.lang.annotations.Language
import spp.jetbrains.marker.jvm.service.JVMArtifactConditionService
import spp.jetbrains.marker.service.ArtifactConditionService

class JVMConditionParserTest : LightJavaCodeInsightFixtureTestCase() {

    override fun tearDown() {
        super.tearDown()
        TestApplicationManager.getInstance().setDataProvider(null)
    }

    fun `test polyadic expression`() {
        @Language("Java") val code = """
                public class Test {
                    private static final int staticVar = 1;
                    private int instanceVar = 2;
                    public void test() {
                        int localVar = 3;
                        System.out.println("done");
                    }
                }
            """.trimIndent()

        ApplicationManager.getApplication().runReadAction {
            val sourceFile = PsiFileFactory.getInstance(project).createFileFromText(
                "Test.java", JavaFileType.INSTANCE, code
            ) as PsiJavaFile

            val context = sourceFile.findElementAt(175) //System.out line
            val expressionText = TextWithImportsImpl.fromXExpression(
                XExpressionImpl(
                    "staticVar == 1 && instanceVar == 2 && localVar == 3",
                    JavaLanguage.INSTANCE, null
                )
            )
            val codeFragment = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(expressionText, context)
                .createCodeFragment(expressionText, context, project)
            assertEquals(
                "staticFields[staticVar] == 1 && fields[instanceVar] == 2 && localVariables[localVar] == 3",
                JVMArtifactConditionService.toLiveConditional(codeFragment)
            )
        }
    }

    fun `test qualified string expression`() {
        @Language("Java") val code = """
                public class Test {
                    public void test() {
                        String s = "hi";
                        System.out.println("done");
                    }
                }
            """.trimIndent()

        ApplicationManager.getApplication().runReadAction {
            val sourceFile = PsiFileFactory.getInstance(project).createFileFromText(
                "Test.java", JavaFileType.INSTANCE, code
            ) as PsiJavaFile

            val context = sourceFile.findElementAt(78) //System.out line
            val expressionText = TextWithImportsImpl.fromXExpression(
                XExpressionImpl(
                    "s.startsWith(\"hi\")",
                    JavaLanguage.INSTANCE, null
                )
            )
            val codeFragment = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(expressionText, context)
                .createCodeFragment(expressionText, context, project)
            assertEquals(
                "localVariables[s].startsWith(\"hi\")",
                JVMArtifactConditionService.toLiveConditional(codeFragment)
            )
        }
    }

    fun `test polyadic expression back to string`() {
        assertEquals(
            "staticVar == 1 && instanceVar == 2 && localVar == 3",
            ArtifactConditionService.fromLiveConditional(
                "staticFields[staticVar] == 1 && fields[instanceVar] == 2 && localVariables[localVar] == 3"
            )
        )
    }
}
