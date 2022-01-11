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
