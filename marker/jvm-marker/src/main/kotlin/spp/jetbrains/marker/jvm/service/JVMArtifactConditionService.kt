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
package spp.jetbrains.marker.jvm.service

import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.psi.*
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import spp.jetbrains.marker.service.define.IArtifactConditionService
import spp.jetbrains.marker.service.define.IArtifactConditionService.ParseRange

/**
 * Used to parse/format JVM instrument conditions.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactConditionService : IArtifactConditionService {

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
