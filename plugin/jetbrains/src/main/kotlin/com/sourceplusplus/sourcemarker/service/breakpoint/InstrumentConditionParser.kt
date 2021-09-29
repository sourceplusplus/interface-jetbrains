package com.sourceplusplus.sourcemarker.service.breakpoint

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object InstrumentConditionParser {

    private val LOCAL_VAR_REGEX = Regex("localVariables\\[(.+)\\]")
    private val FIELD_VAR_REGEX = Regex("fields\\[(.+)\\]")
    private val STATIC_FIELD_VAR_REGEX = Regex("staticFields\\[(.+)\\]")

    fun fromLiveConditional(conditional: String): String {
        var rtnConditional = conditional
        LOCAL_VAR_REGEX.findAll(rtnConditional).forEach {
            rtnConditional = rtnConditional.replace(it.groupValues[0], it.groupValues[1])
        }
        FIELD_VAR_REGEX.findAll(rtnConditional).forEach {
            rtnConditional = rtnConditional.replace(it.groupValues[0], it.groupValues[1])
        }
        STATIC_FIELD_VAR_REGEX.findAll(rtnConditional).forEach {
            rtnConditional = rtnConditional.replace(it.groupValues[0], it.groupValues[1])
        }
        return rtnConditional
    }

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
                if (it.variable.modifierList?.hasModifierProperty("static") == true) {
                    expression = expression.replaceRange(
                        it.textRange.startOffset + varOffset,
                        it.textRange.endOffset + varOffset,
                        "staticFields[" + it.variable.name + "]"
                    )
                    varOffset += "staticFields[]".length
                } else {
                    expression = expression.replaceRange(
                        it.textRange.startOffset + varOffset,
                        it.textRange.endOffset + varOffset,
                        "fields[" + it.variable.name + "]"
                    )
                    varOffset += "fields[]".length
                }
            } else {
                expression = expression.replaceRange(
                    it.textRange.startOffset + varOffset,
                    it.textRange.endOffset + varOffset,
                    "localVariables[" + it.variable.name + "]"
                )
                varOffset += "localVariables[]".length
            }
        }
        return expression
    }

    private data class ParseRange(
        val textRange: TextRange,
        val variable: PsiVariable
    )
}
