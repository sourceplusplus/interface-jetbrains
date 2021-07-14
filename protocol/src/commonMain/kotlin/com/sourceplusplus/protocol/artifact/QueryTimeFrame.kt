package com.sourceplusplus.protocol.artifact

import kotlinx.serialization.Serializable

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Serializable
enum class QueryTimeFrame(val minutes: Int, val id: String) {
    //todo: support custom time frames
    LAST_MINUTE(1, "1_minute"),
    LAST_5_MINUTES(5, "5_minutes"),
    LAST_15_MINUTES(15, "15_minutes"),
    LAST_30_MINUTES(30, "30_minutes"),
    LAST_HOUR(60, "hour"),
    LAST_3_HOURS(60 * 3, "3_hours"); //todo: id = enum name

    val description = id.toUpperCase().replace("_", " ")

    companion object {
        fun valueOf(minutes: Int): QueryTimeFrame {
            for (timeFrame in values()) {
                if (timeFrame.minutes == minutes) {
                    return timeFrame
                }
            }
            throw IllegalArgumentException("No time frame for minutes: $minutes")
        }
    }
}
