package com.sourceplusplus.protocol.instrument

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class ThrottleStep(private val millis: Int) {
    SECOND(1000),
    MINUTE(1000 * 60),
    HOUR(1000 * 60 * 60),
    DAY(1000 * 60 * 60 * 24);

    fun toMillis(duration: Int): Long {
        return millis * duration.toLong()
    }
}
