package com.sourceplusplus.api.model;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.2
 * @since 0.1.0
 */
public enum QueryTimeFrame {

    LAST_15_MINUTES(15),
    LAST_30_MINUTES(30),
    LAST_HOUR(60);

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
