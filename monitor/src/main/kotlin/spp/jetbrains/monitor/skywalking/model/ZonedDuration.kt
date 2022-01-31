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
