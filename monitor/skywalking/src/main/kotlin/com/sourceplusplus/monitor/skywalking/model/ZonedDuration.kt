package com.sourceplusplus.monitor.skywalking.model

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import monitor.skywalking.protocol.type.Duration
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class ZonedDuration(
    val start: ZonedDateTime,
    val stop: ZonedDateTime,
    val step: SkywalkingClient.DurationStep
) {
    fun toDuration(skywalkingClient: SkywalkingClient): Duration {
        return skywalkingClient.getDuration(start, stop, step)
    }
}
