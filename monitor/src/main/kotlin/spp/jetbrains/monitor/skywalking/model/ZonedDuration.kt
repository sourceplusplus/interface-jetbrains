/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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

import monitor.skywalking.protocol.type.Duration
import spp.jetbrains.monitor.skywalking.SkywalkingClient
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
