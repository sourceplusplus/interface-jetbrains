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
package spp.jetbrains.sourcemarker.instrument.log

import com.intellij.util.ui.ColumnInfo
import spp.jetbrains.PluginBundle.message
import spp.protocol.instrument.event.LiveInstrumentEventType
import spp.protocol.instrument.event.LiveInstrumentRemoved
import spp.protocol.instrument.event.LiveLogHit
import spp.protocol.instrument.event.TrackedLiveEvent
import spp.protocol.utils.toPrettyDuration
import java.time.Instant

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogHitColumnInfo(name: String) : ColumnInfo<TrackedLiveEvent, String>(name) {

    override fun getComparator(): Comparator<TrackedLiveEvent>? {
        return when (name) {
            "Time" -> Comparator { t: TrackedLiveEvent, t2: TrackedLiveEvent ->
                val obj1 = if (t.eventType == LiveInstrumentEventType.LOG_HIT) {
                    t as LiveLogHit
                } else if (t.eventType == LiveInstrumentEventType.LOG_REMOVED) {
                    t as LiveInstrumentRemoved
                } else {
                    throw IllegalArgumentException(t.eventType.name)
                }
                val obj2 = if (t2.eventType == LiveInstrumentEventType.LOG_HIT) {
                    t2 as LiveLogHit
                } else if (t2.eventType == LiveInstrumentEventType.LOG_REMOVED) {
                    t2 as LiveInstrumentRemoved
                } else {
                    throw IllegalArgumentException(t2.eventType.name)
                }
                obj1.occurredAt.compareTo(obj2.occurredAt)
            }
            else -> null
        }
    }

    override fun valueOf(event: TrackedLiveEvent): String {
        if (event.eventType == LiveInstrumentEventType.LOG_HIT) {
            val item = event as LiveLogHit
            return when (name) {
                "Message" -> item.logResult.logs.first().toFormattedMessage()
                "Time" ->
                    (Instant.now().toEpochMilli() - item.occurredAt.toEpochMilli())
                        .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        } else {
            val item = event as LiveInstrumentRemoved
            return when (name) {
                "Message" -> item.cause!!.message ?: item.cause!!.exceptionType
                "Time" -> (Instant.now().toEpochMilli() - item.occurredAt.toEpochMilli())
                    .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        }
    }
}
