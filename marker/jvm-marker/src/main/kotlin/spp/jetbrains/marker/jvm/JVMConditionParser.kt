/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.jvm

import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.psi.*
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import spp.jetbrains.marker.AbstractInstrumentConditionParser

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMConditionParser : AbstractInstrumentConditionParser() {

    override fun getCondition(condition: String, context: PsiElement): String {
        val expressionText = TextWithImportsImpl.fromXExpression(
            XExpressionImpl(condition, context.language, null)
        )
        val codeFragment = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(expressionText, context)
            .createCodeFragment(expressionText, context, context.project)
        return toLiveConditional(codeFragment)
    }

    companion object {
        fun toLiveConditional(codeFragment: JavaCodeFragment): String {
            var expression = codeFragment.text
            val variables = mutableListOf<ParseRange>()
            codeFragment.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    if (element is PsiReferenceExpression) {
                        val resolve = element.resolve()
                        if (resolve is PsiVariable && resolve !is PsiCompiledElement) {
                            variables.add(ParseRange(element.textRange, resolve))
                        }
                    }
                }
            })

            var varOffset = 0
            variables.forEach {
                if (it.variable is PsiField) {
                    if ((it.variable as PsiVariable).modifierList?.hasModifierProperty("static") == true) {
                        expression = expression.replaceRange(
                            it.textRange.startOffset + varOffset,
                            it.textRange.endOffset + varOffset,
                            "staticFields[" + (it.variable as PsiVariable).name + "]"
                        )
                        varOffset += "staticFields[]".length
                    } else {
                        expression = expression.replaceRange(
                            it.textRange.startOffset + varOffset,
                            it.textRange.endOffset + varOffset,
                            "fields[" + (it.variable as PsiVariable).name + "]"
                        )
                        varOffset += "fields[]".length
                    }
                } else {
                    expression = expression.replaceRange(
                        it.textRange.startOffset + varOffset,
                        it.textRange.endOffset + varOffset,
                        "localVariables[" + (it.variable as PsiVariable).name + "]"
                    )
                    varOffset += "localVariables[]".length
                }
            }
            return expression
        }
    }
}
