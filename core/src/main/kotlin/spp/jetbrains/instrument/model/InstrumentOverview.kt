/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.instrument.model

import spp.protocol.instrument.LiveInstrumentType
import spp.protocol.instrument.event.LiveInstrumentApplied
import spp.protocol.instrument.event.LiveInstrumentEvent
import spp.protocol.instrument.event.LiveInstrumentRemoved
import java.time.Instant

/**
 * todo: description.
 *
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class InstrumentOverview(
    val events: MutableList<LiveInstrumentEvent> = mutableListOf()
) {
    val firstEvent: Instant
        get() = events.minOf { it.occurredAt }

    val lastEvent: Instant
        get() = events.maxOf { it.occurredAt }

    val source: String
        get() = events.first().instrument.location.let {
            var sourceStr = it.source.substringBefore("(").substringAfterLast(".")
            if (it.line != -1) {
                sourceStr += ":" + it.line
            }
            sourceStr
        }

    val status: String
        get() = if (events.any { it is LiveInstrumentRemoved && it.cause == null }) {
            "Complete"
        } else if (events.any { it is LiveInstrumentRemoved && it.cause != null }) {
            "Error"
        } else if (events.any { it is LiveInstrumentApplied }) {
            "Active"
        } else {
            "Pending"
        }

    val createdBy: String
        get() = events.first().instrument.meta["created_by"].toString()

    val instrumentType: LiveInstrumentType
        get() = events.first().instrument.type

    val instrumentTypeFormatted: String
        get() = instrumentType.name.lowercase().replaceFirstChar { it.titlecase() }

    val instrumentId: String?
        get() = events.first().instrument.id

    val isFinished: Boolean
        get() = events.any { it is LiveInstrumentRemoved }

    fun isRemovable(selfId: String): Boolean =
        events.none { it is LiveInstrumentRemoved } && events.first().instrument.meta["created_by"] == selfId
}
