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

    const val PATTERN_CURLY_BRACES = "\\{|\\}"
    const val EMPTY = ""
    const val SPACE = " "
    const val DOLLAR = "$"

    @JvmStatic
    fun createPattern(scopeVars: List<String>): Pair<Pattern?, Pattern?> {
        var variablePattern: Pattern? = null
        var templateVariablePattern: Pattern? = null
        if (scopeVars.isNotEmpty()) {
            val sb = StringBuilder("(")
            val sbt = StringBuilder("(")
            for (i in scopeVars.indices) {
                sb.append("\\$").append(scopeVars[i])
                sbt.append("\\$\\{").append(scopeVars[i]).append("\\}")
                if (i + 1 < scopeVars.size) {
                    sb.append("|")
                    sbt.append("|")
                }
            }
            sb.append(")(?:\\s|$)")
            sbt.append(")(?:|$)")
            variablePattern = Pattern.compile(sb.toString())
            templateVariablePattern = Pattern.compile(sbt.toString())
        }
        return Pair.create(variablePattern, templateVariablePattern)
    }

    @JvmStatic
    fun extractVariables(patternPair: Pair<Pattern?, Pattern?>, logText: String): Pair<String, List<String>> {
        var logTemplate = logText
        val varMatches: MutableList<String> = ArrayList()
        if (patternPair.first != null) {
            val m = patternPair.first!!.matcher(logTemplate)
            var matchLength = 0
            while (m.find()) {
                val variable = m.group(1)
                logTemplate = (logTemplate.substring(0, m.start() - matchLength)
                        + logTemplate.substring(m.start() - matchLength)
                    .replaceFirst(Pattern.quote(variable).toRegex(), "{}"))
                matchLength = matchLength + variable.length - 1
                varMatches.add(variable)
            }
        }
        if (patternPair.second != null) {
            val m = patternPair.second!!.matcher(logTemplate)
            var matchLength = 0
            while (m.find()) {
                var variable = m.group(1)
                logTemplate = (logTemplate.substring(0, m.start() - matchLength)
                        + logTemplate.substring(m.start() - matchLength)
                    .replaceFirst(Pattern.quote(variable).toRegex(), "{}"))
                matchLength = matchLength + variable.length - 1
                variable = variable.replace(PATTERN_CURLY_BRACES.toRegex(), EMPTY)
                varMatches.add(variable)
            }
        }
        return Pair.create(logTemplate, varMatches)
    }

    fun matchVariables(patternPair: Pair<Pattern?, Pattern?>, input: String, function: Function<Matcher, Any>) {
        if (patternPair.first != null) {
            val match = patternPair.first!!.matcher(input)
            function.apply(match)
        }
        if (patternPair.second != null) {
            val match = patternPair.second!!.matcher(input)
            function.apply(match)
        }
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
