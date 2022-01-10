package spp.jetbrains

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import spp.jetbrains.sourcemarker.VariableParser
import java.util.ArrayList;
import java.util.regex.Pattern

internal class VariableParserTest {

    var variablePattern =
        Pattern.compile("(\\\$tt|\\\$deleteDate|\\\$executorService|\\\$FIVE_MINUTES_MILLIS|\\\$mapTodos|\\\$GET|\\\$HEAD|\\\$POST|\\\$PUT|\\\$PATCH|\\\$DELETE|\\\$OPTIONS|\\\$TRACE)(?:\\s|$)")
    var templateVariablePattern =
        Pattern.compile("(\\\$\\{tt}|\\\$\\{deleteDate}|\\\$\\{executorService}|\\\$\\{FIVE_MINUTES_MILLIS}|\\\$\\{mapTodos}|\\\$\\{GET}|\\\$\\{HEAD}|\\\$\\{POST}|\\\$\\{PUT}|\\\$\\{PATCH}|\\\$\\{DELETE}|\\\$\\{OPTIONS}|\\\$\\{TRACE})(?:|$)")

    @Test
    fun extractVariablesPattern1() {
        val varMatches = ArrayList<String>()
        var pattern = "hello\$tt \$tthello \${tt}\$tt"
        pattern = VariableParser.extractVariables(variablePattern, templateVariablePattern, varMatches, pattern)
        Assertions.assertEquals(3, varMatches.size.toLong())
        Assertions.assertEquals("hello{} \$tthello {}{}", pattern)
    }

    @Test
    fun extractVariablesPattern2() {
        val varMatches = ArrayList<String>()
        var pattern = "\$tt \$deleteDate \${tt}\${deleteDate}\$tt"
        pattern = VariableParser.extractVariables(variablePattern, templateVariablePattern, varMatches, pattern)
        Assertions.assertEquals(5, varMatches.size.toLong())
        Assertions.assertEquals("{} {} {}{}{}", pattern)
    }

    @Test
    fun extractVariablesPattern3() {
        val varMatches = ArrayList<String>()
        var pattern: String? = "hello\$tt \$deleteDatebut not\${tt}working\${deleteDate}\$tt"
        pattern = VariableParser.extractVariables(variablePattern, templateVariablePattern, varMatches, pattern)
        Assertions.assertEquals(4, varMatches.size.toLong())
        Assertions.assertEquals("hello{} \$deleteDatebut not{}working{}{}", pattern)
    }
}