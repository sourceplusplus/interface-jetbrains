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
package spp.jetbrains.sourcemarker.service.log

import com.intellij.util.ui.ColumnInfo
import spp.protocol.instrument.LiveInstrumentEvent
import spp.protocol.instrument.LiveInstrumentEventType
import spp.protocol.instrument.log.event.LiveLogHit
import spp.protocol.utils.toPrettyDuration
import spp.jetbrains.sourcemarker.PluginBundle.message
import io.vertx.core.json.Json
import kotlinx.datetime.Clock
import spp.protocol.instrument.LiveInstrumentRemoved

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
                    Json.decodeValue(t.data, LiveLogHit::class.java)
                } else if (t.eventType == LiveInstrumentEventType.LOG_REMOVED) {
                    Json.decodeValue(t.data, LiveInstrumentRemoved::class.java)
                } else {
                    throw IllegalArgumentException(t.eventType.name)
                }
                val obj2 = if (t2.eventType == LiveInstrumentEventType.LOG_HIT) {
                    Json.decodeValue(t2.data, LiveLogHit::class.java)
                } else if (t2.eventType == LiveInstrumentEventType.LOG_REMOVED) {
                    Json.decodeValue(t2.data, LiveInstrumentRemoved::class.java)
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
            val item = Json.decodeValue(event.data, LiveLogHit::class.java)
            return when (name) {
                "Message" -> item.logResult.logs.first().toFormattedMessage()
                "Time" ->
                    (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                        .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        } else {
            val item = Json.decodeValue(event.data, LiveInstrumentRemoved::class.java)
            return when (name) {
                "Message" -> item.cause!!.message ?: item.cause!!.exceptionType
                "Time" -> (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                    .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        }
    }
}
