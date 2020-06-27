package com.sourceplusplus.api.model;

/**
 * Time frames the core supports for artifact metrics/traces.
 * todo: remove class for a more dynamic solution
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.1.0
 */
public enum QueryTimeFrame {

    LAST_5_MINUTES(5),
    LAST_15_MINUTES(15),
    LAST_30_MINUTES(30),
    LAST_HOUR(60),
    LAST_3_HOURS(60 * 3);

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
