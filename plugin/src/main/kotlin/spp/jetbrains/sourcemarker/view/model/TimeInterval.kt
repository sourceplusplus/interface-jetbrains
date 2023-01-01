/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.sourcemarker.view.model

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TimeInterval(
    var refreshInterval: Long,
    val keepSize: Int,
    val keepTimeSize: Long,
    val xStepSize: Long
) {
    LAST_5_MINUTES(500L, 10 * 6 * 5, 10_000L * 6 * 5, 10_000L * 6),
    LAST_15_MINUTES(1000L, 10 * 6 * 15, 10_000L * 6 * 15, 10_000L * 6 * 3),
    LAST_30_MINUTES(5000L, 10 * 6 * 30, 10_000L * 6 * 30, 10_000L * 6 * 6),
    LAST_1_HOUR(5000L, 10 * 6 * 60, 10_000L * 6 * 60, 10_000L * 6 * 12),
    LAST_4_HOURS(5000L, 10 * 6 * 60 * 4, 10_000L * 6 * 60 * 4, 10_000L * 6 * 60 * 2),
    LAST_12_HOURS(5000L, 10 * 6 * 60 * 12, 10_000L * 6 * 60 * 12, 10_000L * 6 * 60 * 6),
    LAST_24_HOURS(5000L, 10 * 6 * 60 * 24, 10_000L * 6 * 60 * 24, 10_000L * 6 * 60 * 12)
}
