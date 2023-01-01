/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.instrument.log

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
        scopeVars: List<String>,
        varInitChar: String = DOLLAR,
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
        val variable = text.lowercase().substringAfterLast(SPACE)
        return ((variable.startsWith(DOLLAR) && variable.substring(1) != v
                && v.lowercase().contains(variable.substring(1)))
                || (variable.startsWith("\${") && variable.substring(2) != v
                && v.lowercase().contains(variable.substring(2))))
    }
}
