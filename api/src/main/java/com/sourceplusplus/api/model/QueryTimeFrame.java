package com.sourceplusplus.api.model;

/**
 * Time frames the core supports for artifact metrics/traces.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
public enum QueryTimeFrame {

    LAST_5_MINUTES(5),
    LAST_15_MINUTES(15),
    LAST_30_MINUTES(30),
    LAST_HOUR(60),
    LAST_3_HOURS(60 * 3);
//    LAST_6_HOURS(60 * 6),
//    LAST_12_HOURS(60 * 12),
//    LAST_24_HOURS(60 * 24);

    private final int minutes;

    QueryTimeFrame(int minutes) {
        this.minutes = minutes;
    }

    public int getMinutes() {
        return minutes;
    }

    public static QueryTimeFrame valueOf(int minutes) {
        for (QueryTimeFrame timeFrame : QueryTimeFrame.values()) {
            if (timeFrame.minutes == minutes) {
                return timeFrame;
            }
        }
        throw new IllegalArgumentException("No time frame for minutes: " + minutes);
    }
}
