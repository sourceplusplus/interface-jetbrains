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
package spp.jetbrains.sourcemarker.service.log

import com.intellij.openapi.util.Pair
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

object VariableParser {

    const val EMPTY = ""
    const val SPACE = " "
    const val DOLLAR = "$"

    @JvmStatic
    fun createPattern(
        scopeVars: List<String>, varInitChar: String = DOLLAR,
        includeCurlyPattern: Boolean = true,
        ignoreCase: Boolean = false
    ): Pattern {
        val sb = StringBuilder("")
        if (scopeVars.isNotEmpty()) {
            sb.append("(")
            for (i in scopeVars.indices) {
                if (varInitChar.isNotEmpty()) {
                    sb.append("\\$varInitChar")
                }
                sb.append(scopeVars[i]).append("(?=\\s|$)")
                if (includeCurlyPattern) {
                    sb.append("|")
                    sb.append("\\$varInitChar\\{").append(scopeVars[i]).append("\\}")
                }
                if (i + 1 < scopeVars.size) {
                    sb.append("|")
                }
            }
            sb.append(")")
        }

        return if (ignoreCase) {
            Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile(sb.toString())
        }
    }

    @JvmStatic
    fun extractVariables(pattern: Pattern, logText: String): Pair<String, List<String>> {
        var logTemplate = logText
        val varMatches: MutableList<String> = ArrayList()
        if (pattern.pattern().isNotEmpty()) {
            val m = pattern.matcher(logTemplate)
            var matchLength = 0
            while (m.find()) {
                val variable = m.group(1)
                logTemplate = (logTemplate.substring(0, m.start() - matchLength)
                        + logTemplate.substring(m.start() - matchLength)
                    .replaceFirst(Pattern.quote(variable).toRegex(), "{}"))
                matchLength = matchLength + variable.length - 1

                if (variable.startsWith("$DOLLAR{")) {
                    varMatches.add(variable.substring(2, variable.length - 1))
                } else {
                    varMatches.add(variable.substring(1))
                }
            }
        }
        return Pair.create(logTemplate, varMatches)
    }

    fun matchVariables(pattern: Pattern, input: String, function: Function<Matcher, Any>) {
        val match = pattern.matcher(input)
        function.apply(match)
    }

    @JvmStatic
    fun isVariable(text: String, v: String): Boolean {
        val variable = text.toLowerCase().substringAfterLast(SPACE)
        return ((variable.startsWith(DOLLAR) && variable.substring(1) != v
                && v.toLowerCase().contains(variable.substring(1)))
                || (variable.startsWith("\${") && variable.substring(2) != v
                && v.toLowerCase().contains(variable.substring(2))))
    }
}
