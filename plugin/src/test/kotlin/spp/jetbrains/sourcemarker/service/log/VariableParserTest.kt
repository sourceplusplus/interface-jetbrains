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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VariableParserTest {

    @Test
    fun extractVariablesPattern1() {
        val input = "hello\$tt \$tthello \${tt}\$tt"
        val patternPair = VariableParser.createPattern(listOf("tt", "deleteDate", "executorService"))
        val resp = VariableParser.extractVariables(patternPair, input)

        assertEquals(3, resp.second.size)
        assertEquals("hello{} \$tthello {}{}", resp.first)
    }

    @Test
    fun extractVariablesPattern2() {
        val input = "\$tt \$deleteDate \${tt}\${deleteDate}\$tt"
        val patternPair = VariableParser.createPattern(listOf("tt", "deleteDate", "executorService"))
        val resp = VariableParser.extractVariables(patternPair, input)

        assertEquals(5, resp.second.size)
        assertEquals("{} {} {}{}{}", resp.first)
    }

    @Test
    fun extractVariablesPattern3() {
        val input = "hello\$user \$deleteDatebut not\${user}working\${deleteDate}\$user"
        val patternPair = VariableParser.createPattern(listOf("user", "deleteDate", "executorService"))
        val resp = VariableParser.extractVariables(patternPair, input)

        assertEquals(4, resp.second.size)
        assertEquals("hello{} \$deleteDatebut not{}working{}{}", resp.first)
    }

    @Test
    fun extractNoVariables() {
        val input = "hello\$user \$deleteDatebut not\${user}working\${deleteDate}\$user"
        val patternPair = VariableParser.createPattern(emptyList())
        val resp = VariableParser.extractVariables(patternPair, input)

        assertEquals(0, resp.second.size)
        assertEquals(input, resp.first)
    }
}
