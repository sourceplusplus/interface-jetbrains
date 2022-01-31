/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.monitor.skywalking.model

import spp.jetbrains.monitor.skywalking.SkywalkingClient.DurationStep
import kotlinx.datetime.Instant
import java.time.Duration
import java.time.Period
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class GetMultipleEndpointMetrics(
    val metricId: String,
    val endpointId: String,
    val numOfLinear: Int,
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
