/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.instrument.log

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VariableParserTest {

    @Test
    fun extractVariablesPattern1() {
        val input = "hello\$tt \$tthello \${tt}\$tt"
        val varPattern = VariableParser.createPattern(listOf("tt", "deleteDate", "executorService"))
        val resp = VariableParser.extractVariables(varPattern, input)

        assertEquals(3, resp.second.size)
        assertEquals("hello{} \$tthello {}{}", resp.first)
    }

    @Test
    fun extractVariablesPattern2() {
        val input = "\$tt \$deleteDate \${tt}\${deleteDate}\$tt"
        val varPattern = VariableParser.createPattern(listOf("tt", "deleteDate", "executorService"))
        val resp = VariableParser.extractVariables(varPattern, input)

        assertEquals(5, resp.second.size)
        assertEquals("{} {} {}{}{}", resp.first)
    }

    @Test
    fun extractVariablesPattern3() {
        val input = "hello\$user \$deleteDatebut not\${user}working\${deleteDate}\$user"
        val varPattern = VariableParser.createPattern(listOf("user", "deleteDate", "executorService"))
        val resp = VariableParser.extractVariables(varPattern, input)

        assertEquals(4, resp.second.size)
        assertEquals("hello{} \$deleteDatebut not{}working{}{}", resp.first)
    }

    @Test
    fun testVariableOrder() {
        val input = "\${i}hi\$date"
        val variablePattern = VariableParser.createPattern(listOf("i", "deleteDate", "date"))
        val resp = VariableParser.extractVariables(variablePattern, input)

        assertEquals(2, resp.second.size)
        assertEquals("{}hi{}", resp.first)
        assertEquals(listOf("i", "date"), resp.second)
    }

    @Test
    fun extractNoVariables() {
        val input = "hello\$user \$deleteDatebut not\${user}working\${deleteDate}\$user"
        val varPattern = VariableParser.createPattern(emptyList())
        val resp = VariableParser.extractVariables(varPattern, input)

        assertEquals(0, resp.second.size)
        assertEquals(input, resp.first)
    }
}
