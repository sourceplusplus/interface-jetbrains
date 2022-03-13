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
package spp.jetbrains.sourcemarker.service.instrument.log

import com.intellij.util.ui.ColumnInfo
import io.vertx.core.json.JsonObject
import kotlinx.datetime.Clock
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.protocol.instrument.event.LiveInstrumentEvent
import spp.protocol.instrument.event.LiveInstrumentEventType
import spp.protocol.marshall.ProtocolMarshaller.deserializeLiveInstrumentRemoved
import spp.protocol.marshall.ProtocolMarshaller.deserializeLiveLogHit
import spp.protocol.utils.toPrettyDuration

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogHitColumnInfo(name: String) : ColumnInfo<LiveInstrumentEvent, String>(name) {

    override fun getComparator(): Comparator<LiveInstrumentEvent>? {
        return when (name) {
            "Time" -> Comparator { t: LiveInstrumentEvent, t2: LiveInstrumentEvent ->
                val obj1 = if (t.eventType == LiveInstrumentEventType.LOG_HIT) {
                    deserializeLiveLogHit(JsonObject(t.data))
                } else if (t.eventType == LiveInstrumentEventType.LOG_REMOVED) {
                    deserializeLiveInstrumentRemoved(JsonObject(t.data))
                } else {
                    throw IllegalArgumentException(t.eventType.name)
                }
                val obj2 = if (t2.eventType == LiveInstrumentEventType.LOG_HIT) {
                    deserializeLiveLogHit(JsonObject(t2.data))
                } else if (t2.eventType == LiveInstrumentEventType.LOG_REMOVED) {
                    deserializeLiveInstrumentRemoved(JsonObject(t2.data))
                } else {
                    throw IllegalArgumentException(t2.eventType.name)
                }
                obj1.occurredAt.compareTo(obj2.occurredAt)
            }
            else -> null
        }
    }

    override fun valueOf(event: LiveInstrumentEvent): String {
        if (event.eventType == LiveInstrumentEventType.LOG_HIT) {
            val item = deserializeLiveLogHit(JsonObject(event.data))
            return when (name) {
                "Message" -> item.logResult.logs.first().toFormattedMessage()
                "Time" ->
                    (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                        .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        } else {
            val item = deserializeLiveInstrumentRemoved(JsonObject(event.data))
            return when (name) {
                "Message" -> item.cause!!.message ?: item.cause!!.exceptionType
                "Time" -> (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                    .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        }
    }
}