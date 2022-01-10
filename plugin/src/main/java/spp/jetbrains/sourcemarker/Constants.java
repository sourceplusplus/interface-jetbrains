package spp.jetbrains.sourcemarker;

import java.util.regex.Pattern;

public interface Constants {
    String PATTERN_CURLY_BRACES = "\\{|\\}";
    String EMPTY = "";
    String DOLLAR = "$";
    String MESSAGE = "Message";
    String TIME = "Time";
    String SPACE = " ";
    String WAITING_FOR_LIVE_LOG_DATA = "Waiting for live log data...";
    String QUOTE_CURLY_BRACES = Pattern.quote("{}");
}
