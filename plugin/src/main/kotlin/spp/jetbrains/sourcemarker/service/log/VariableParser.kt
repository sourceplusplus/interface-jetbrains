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
import spp.protocol.instrument.LiveLog
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

object VariableParser {

    const val PATTERN_CURLY_BRACES = "\\{|\\}"
    const val EMPTY = ""
    const val SPACE = " "
    const val DOLLAR = "$"
    val QUOTE_CURLY_BRACES = Pattern.quote("{}")
    val QUOTE_SQUARE_BRACES = Pattern.quote("[]")

    @JvmStatic
    fun createPattern(scopeVars: List<String>): Pattern? {
        var variablePattern: Pattern? = null
        if (scopeVars.isNotEmpty()) {
            val sb = StringBuilder("(")
            for (i in scopeVars.indices) {
                sb.append("\\$").append(scopeVars[i]).append(" ")
                sb.append("|")
                sb.append("\\$\\{").append(scopeVars[i]).append("\\}")
                if (i + 1 < scopeVars.size) {
                    sb.append("|")
                }
            }
            sb.append(")(?:|$)")
            variablePattern = Pattern.compile(sb.toString())
        }
        return variablePattern
    }

    @JvmStatic
    fun extractVariables(pattern: Pattern?, logText: String): Pair<String, List<String>> {
        var logTemplate = "$logText "
        val varMatches: MutableList<String> = ArrayList()
        pattern?.let {
            val m = it.matcher(logTemplate)
            var matchLength = 0
            while (m.find()) {
                var variable = m.group(1).trim()

                if (variable.contains("{")) {
                    logTemplate = (logTemplate.substring(0, m.start() - matchLength)
                            + logTemplate.substring(m.start() - matchLength)
                        .replaceFirst(Pattern.quote(variable).toRegex(), "[]"))
                    matchLength = matchLength + variable.length - 1
                    variable = variable.replace(PATTERN_CURLY_BRACES.toRegex(), EMPTY)
                } else {
                    logTemplate = (logTemplate.substring(0, m.start() - matchLength)
                            + logTemplate.substring(m.start() - matchLength)
                        .replaceFirst(Pattern.quote(variable).toRegex(), "{}"))
                    matchLength = matchLength + variable.length - 1
                }
                varMatches.add(variable)
            }
        }
        return Pair.create(logTemplate.trim(), varMatches)
    }

    fun matchVariables(patternPair: Pattern?, input: String, function: Function<Matcher, Any>) {
        patternPair?.let {
            val match = it.matcher(input)
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

    @JvmStatic
    fun createOriginalMessage(liveLog: LiveLog): String {
        var originalMessage = liveLog.logFormat
        for (arg in liveLog.logArguments) {
            val pattern = findFirstPattern(originalMessage)
            if (pattern == QUOTE_CURLY_BRACES) {
                originalMessage = originalMessage.replaceFirst(
                    findFirstPattern(originalMessage).toRegex(),
                    Matcher.quoteReplacement("$DOLLAR$arg")
                )
            } else {
                originalMessage = originalMessage.replaceFirst(
                    findFirstPattern(originalMessage).toRegex(),
                    Matcher.quoteReplacement("$DOLLAR{$arg}")
                )
            }

        }
        return originalMessage
    }

    private fun findFirstPattern(message: String): String {
        val indexOfSquareB = message.indexOf("[]")
        if (indexOfSquareB == -1)
            return QUOTE_CURLY_BRACES
        val indexOfCurlyB = message.indexOf("{}")
        if (indexOfCurlyB == -1)
            return QUOTE_SQUARE_BRACES
        return if (indexOfSquareB < indexOfCurlyB) QUOTE_SQUARE_BRACES else QUOTE_CURLY_BRACES
    }
}
