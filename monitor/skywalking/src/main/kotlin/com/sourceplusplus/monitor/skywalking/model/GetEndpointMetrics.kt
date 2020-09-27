package com.sourceplusplus.monitor.skywalking.model

import com.sourceplusplus.monitor.skywalking.SkywalkingClient.DurationStep
import kotlinx.datetime.Instant
import java.time.Duration
import java.time.Period
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class GetEndpointMetrics(
    val metricIds: List<String>,
    val endpointId: String,
    val zonedDuration: ZonedDuration
) {
    fun toZonedTimes(): List<ZonedDateTime> {
        val zonedTimes: MutableList<ZonedDateTime> = ArrayList()
        var startTimestamp = zonedDuration.start
        val step = when (zonedDuration.step) {
            DurationStep.SECOND -> Duration.ofSeconds(1)
            DurationStep.MINUTE -> Duration.ofMinutes(1)
            DurationStep.HOUR -> Duration.ofHours(1)
            DurationStep.DAY -> Period.ofDays(1)
        }

        while (startTimestamp.isBefore(zonedDuration.stop)) {
            zonedTimes.add(startTimestamp)
            startTimestamp = startTimestamp.plus(step)
        }
        return zonedTimes.toList()
    }

    fun toInstantTimes(): List<Instant> {
        return toZonedTimes().map { Instant.fromEpochMilliseconds(it.toInstant().toEpochMilli()) }
    }
}
