package spp.jetbrains.sourcemarker;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableParser {
    private static final String PATTERN_CURLY_BRACES = "\\{|\\}";
    private static final String EMPTY = "";

    public static String extractVariables(Pattern variablePattern, Pattern templateVariablePattern,
                                    ArrayList<String> varMatches, String logPattern) {
        if (variablePattern != null) {
            Matcher m = variablePattern.matcher(logPattern);
            int matchLength = 0;
            while (m.find()) {
                String var = m.group(1);
                logPattern = logPattern.substring(0, m.start() - matchLength)
                        + logPattern.substring(m.start() - matchLength).replaceFirst(Pattern.quote(var), "{}");
                //logPattern = logPattern.replaceFirst(Pattern.quote(var), "{}");
                matchLength = matchLength + var.length() - 1;
                varMatches.add(var);
            }
        }

        if (templateVariablePattern != null) {
            Matcher m = templateVariablePattern.matcher(logPattern);
            int matchLength = 0;
            while (m.find()) {
                String var = m.group(1);
                logPattern = logPattern.substring(0, m.start() - matchLength)
                        + logPattern.substring(m.start() - matchLength).replaceFirst(Pattern.quote(var), "{}");
                //logPattern = logPattern.replaceFirst(Pattern.quote(var), "{}");
                matchLength = matchLength + var.length() - 1;
                var = var.replaceAll(PATTERN_CURLY_BRACES, EMPTY);
                varMatches.add(var);
            }
        }
        return logPattern;
    }
}
