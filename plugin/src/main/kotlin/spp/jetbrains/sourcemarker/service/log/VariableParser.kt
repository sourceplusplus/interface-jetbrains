package spp.jetbrains.sourcemarker;

import com.intellij.openapi.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableParser {

    public static final String PATTERN_CURLY_BRACES = "\\{|\\}";
    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String DOLLAR = "$";

    public static Pair<Pattern, Pattern> createPattern(List<String> scopeVars) {
        Pattern variablePattern = null;
        Pattern templateVariablePattern = null;
        if (!scopeVars.isEmpty()) {
            StringBuilder sb = new StringBuilder("(");
            StringBuilder sbt = new StringBuilder("(");
            for (int i = 0; i < scopeVars.size(); i++) {
                sb.append("\\$").append(scopeVars.get(i));
                sbt.append("\\$\\{").append(scopeVars.get(i)).append("\\}");
                if (i + 1 < scopeVars.size()) {
                    sb.append("|");
                    sbt.append("|");
                }
            }
            sb.append(")(?:\\s|$)");
            sbt.append(")(?:|$)");
            variablePattern = Pattern.compile(sb.toString());
            templateVariablePattern = Pattern.compile(sbt.toString());
        }
        return Pair.create(variablePattern, templateVariablePattern);
    }

    public static Pair<String, List<String>> extractVariables(Pair<Pattern, Pattern> patternPair, String logPattern) {
        List<String> varMatches = new ArrayList<>();
        if (patternPair.first != null) {
            Matcher m = patternPair.first.matcher(logPattern);
            int matchLength = 0;
            while (m.find()) {
                String var = m.group(1);
                logPattern = logPattern.substring(0, m.start() - matchLength)
                        + logPattern.substring(m.start() - matchLength).replaceFirst(Pattern.quote(var), "{}");
                matchLength = matchLength + var.length() - 1;
                varMatches.add(var);
            }
        }

        if (patternPair.second != null) {
            Matcher m = patternPair.second.matcher(logPattern);
            int matchLength = 0;
            while (m.find()) {
                String var = m.group(1);
                logPattern = logPattern.substring(0, m.start() - matchLength)
                        + logPattern.substring(m.start() - matchLength).replaceFirst(Pattern.quote(var), "{}");
                matchLength = matchLength + var.length() - 1;
                var = var.replaceAll(PATTERN_CURLY_BRACES, EMPTY);
                varMatches.add(var);
            }
        }
        return Pair.create(logPattern, varMatches);
    }

    public static void matchVariables(Pair<Pattern, Pattern> patternPair, String input, Function<Matcher, Object> function) {
        if (patternPair.first != null) {
            Matcher match = patternPair.first.matcher(input);
            function.apply(match);
        }

        if (patternPair.second != null) {
            Matcher match = patternPair.second.matcher(input);
            function.apply(match);
        }
    }

    public static boolean isVariable(String text, String v) {
        String var = substringAfterLast(SPACE, text.toLowerCase());

        return (var.startsWith(DOLLAR) && !var.substring(1).equals(v)
                && v.toLowerCase().contains(var.substring(1)))
                || (var.startsWith("${") && !var.substring(2).equals(v)
                && v.toLowerCase().contains(var.substring(2)));
    }

    public static String substringAfterLast(String delimiter, String value) {
        int index = value.lastIndexOf(delimiter);
        if (index == -1) return value;
        else return value.substring(index + delimiter.length());
    }
}
