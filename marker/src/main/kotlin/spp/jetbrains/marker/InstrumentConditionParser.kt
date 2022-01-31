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
package spp.jetbrains.marker

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
