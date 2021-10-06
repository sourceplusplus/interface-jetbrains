package com.sourceplusplus.marker

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class InstrumentConditionParser {

    companion object {
        private val LOCAL_VAR_REGEX: Regex = Regex("localVariables\\[(.+)\\]")
        private val FIELD_VAR_REGEX: Regex = Regex("fields\\[(.+)\\]")
        private val STATIC_FIELD_VAR_REGEX: Regex = Regex("staticFields\\[(.+)\\]")

        @JvmStatic
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
    }

    data class ParseRange(
        val textRange: TextRange,
        val variable: PsiElement
    )

    abstract fun getCondition(condition: String, context: PsiElement): String
}
