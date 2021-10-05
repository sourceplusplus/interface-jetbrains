package com.sourceplusplus.sourcemarker.service.breakpoint

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
import com.sourceplusplus.marker.InstrumentConditionParser
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InstrumentConditionParserTest : LightJavaCodeInsightFixtureTestCase() {

    @BeforeEach
    override fun setUp() = super.setUp()

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        TestApplicationManager.getInstance().setDataProvider(null)
    }

    @Test
    fun `polyadic expression`() {
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
                InstrumentConditionParser.toLiveConditional(codeFragment)
            )
        }
    }

    @Test
    fun `qualified string expression`() {
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
                InstrumentConditionParser.toLiveConditional(codeFragment)
            )
        }
    }

    @Test
    fun `polyadic expression back to string`() {
        assertEquals(
            "staticVar == 1 && instanceVar == 2 && localVar == 3",
            InstrumentConditionParser.fromLiveConditional(
                "staticFields[staticVar] == 1 && fields[instanceVar] == 2 && localVariables[localVar] == 3"
            )
        )
    }
}
