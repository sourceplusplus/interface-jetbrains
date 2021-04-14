package com.sourceplusplus.sourcemarker.service.hindsight

import com.intellij.psi.*
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object BreakpointConditionParser {

    private val log = LoggerFactory.getLogger(BreakpointConditionParser::class.java)

    fun toHindsightConditional(codeFragment: JavaCodeFragment): String {
        var expression = codeFragment.text
        val identifiers = mutableListOf<PsiReferenceExpression>()
        codeFragment.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is PsiReferenceExpression) {
                    identifiers.add(element)
                }
            }
        })

        identifiers.forEach {
            val declaration = it.resolve()
            if (declaration != null) {
                expression = if (declaration is PsiField) {
                    if (declaration.modifierList?.hasModifierProperty("static") == true) {
                        expression.replace(it.text, "staticFields[" + it.text + "]")
                    } else {
                        expression.replace(it.text, "fields[" + it.text + "]")
                    }
                } else {
                    expression.replace(it.text, "localVariables[" + it.text + "]")
                }
            } else {
                log.warn("Could not find declaration for identifier: $it")
            }
        }
        return expression
    }
}
