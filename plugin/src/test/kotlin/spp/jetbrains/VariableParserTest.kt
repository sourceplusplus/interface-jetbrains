package spp.jetbrains

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import spp.jetbrains.sourcemarker.VariableParser

internal class VariableParserTest {

    @Test
    fun extractVariablesPattern1() {
        var input = "hello\$tt \$tthello \${tt}\$tt"

        var patternPair = VariableParser.createPattern(listOf("tt", "deleteDate", "executorService"))
        var resp = VariableParser.extractVariables(patternPair, input)
        Assertions.assertEquals(3, resp.second.size)
        Assertions.assertEquals("hello{} \$tthello {}{}", resp.first)
    }

    @Test
    fun extractVariablesPattern2() {
        var input = "\$tt \$deleteDate \${tt}\${deleteDate}\$tt"
        var patternPair = VariableParser.createPattern(listOf("tt", "deleteDate", "executorService"))
        var resp = VariableParser.extractVariables(patternPair, input)

        Assertions.assertEquals(5, resp.second.size)
        Assertions.assertEquals("{} {} {}{}{}", resp.first)
    }

    @Test
    fun extractVariablesPattern3() {
        var input = "hello\$user \$deleteDatebut not\${user}working\${deleteDate}\$user"
        var patternPair = VariableParser.createPattern(listOf("user", "deleteDate", "executorService"))
        var resp = VariableParser.extractVariables(patternPair, input)
        Assertions.assertEquals(4, resp.second.size)
        Assertions.assertEquals("hello{} \$deleteDatebut not{}working{}{}", resp.first)
    }
}