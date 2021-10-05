package com.sourceplusplus.marker.jvm

import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object JVMConditionParser {

    fun getCondition(condition: String, context: PsiElement): String {
        val expressionText = TextWithImportsImpl.fromXExpression(
            XExpressionImpl(condition, context.language, null)
        )
        val codeFragment = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(expressionText, context)
            .createCodeFragment(expressionText, context, context.project)
        return InstrumentConditionParser.toLiveConditional(codeFragment)
    }
}
