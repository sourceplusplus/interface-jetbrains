package com.sourceplusplus.sourcemarker.service.breakpoint

import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import org.intellij.lang.annotations.Language
import org.junit.Test

class BreakpointConditionParserTest : LightPlatformCodeInsightFixture4TestCase() {

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
            BreakpointConditionParser.toBreakpointConditional(codeFragment)
        )
    }
}
