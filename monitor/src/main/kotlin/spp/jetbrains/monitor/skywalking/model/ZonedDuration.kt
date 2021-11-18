package spp.jetbrains.monitor.skywalking.model

import spp.jetbrains.monitor.skywalking.SkywalkingClient
import monitor.skywalking.protocol.type.Duration
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class ZonedDuration(
    val start: ZonedDateTime,
    val stop: ZonedDateTime,
    val step: SkywalkingClient.DurationStep
) {
    fun toDuration(skywalkingClient: SkywalkingClient): Duration {
        //minus on stop as skywalking stop is inclusive
        return when (step) {
            SkywalkingClient.DurationStep.SECOND -> skywalkingClient.getDuration(start, stop.minusSeconds(1), step)
            SkywalkingClient.DurationStep.MINUTE -> skywalkingClient.getDuration(start, stop.minusMinutes(1), step)
            SkywalkingClient.DurationStep.HOUR -> skywalkingClient.getDuration(start, stop.minusHours(1), step)
            SkywalkingClient.DurationStep.DAY -> skywalkingClient.getDuration(start, stop.minusDays(1), step)
        }
    }
}
