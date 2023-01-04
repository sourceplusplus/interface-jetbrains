/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.monitor.skywalking.model

import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.1.0
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
        return toZonedTimes().map { it.toInstant() }
    }
}
