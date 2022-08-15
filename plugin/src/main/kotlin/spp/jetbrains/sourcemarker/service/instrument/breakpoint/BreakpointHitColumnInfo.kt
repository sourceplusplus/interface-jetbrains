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
package spp.jetbrains.sourcemarker.service.instrument.breakpoint

import com.intellij.util.ui.ColumnInfo
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import kotlinx.datetime.Clock
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.protocol.instrument.event.LiveInstrumentEvent
import spp.protocol.instrument.event.LiveInstrumentEventType
import spp.protocol.marshall.ProtocolMarshaller
import spp.protocol.marshall.ProtocolMarshaller.deserializeLiveInstrumentRemoved
import spp.protocol.utils.toPrettyDuration

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class BreakpointHitColumnInfo(name: String) : ColumnInfo<LiveInstrumentEvent, String>(name) {

    override fun getComparator(): Comparator<LiveInstrumentEvent>? {
        return when (name) {
            "Time" -> Comparator { t: LiveInstrumentEvent, t2: LiveInstrumentEvent ->
                val obj1 = if (t.eventType == LiveInstrumentEventType.BREAKPOINT_HIT) {
                    ProtocolMarshaller.deserializeLiveBreakpointHit(JsonObject(t.data))
                } else if (t.eventType == LiveInstrumentEventType.BREAKPOINT_REMOVED) {
                    deserializeLiveInstrumentRemoved(JsonObject(t.data))
                } else {
                    throw IllegalArgumentException(t.eventType.name)
                }
                val obj2 = if (t2.eventType == LiveInstrumentEventType.BREAKPOINT_HIT) {
                    ProtocolMarshaller.deserializeLiveBreakpointHit(JsonObject(t2.data))
                } else if (t2.eventType == LiveInstrumentEventType.BREAKPOINT_REMOVED) {
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
        val breakpointData = mutableListOf<Map<String, Any?>>()
        if (event.eventType == LiveInstrumentEventType.BREAKPOINT_HIT) {
            val item = ProtocolMarshaller.deserializeLiveBreakpointHit(JsonObject(event.data))
            item.stackTrace.elements.first().variables.forEach {
                breakpointData.add(mapOf(it.name to it.value))
            }
            return when (name) {
                "Breakpoint Data" -> Json.encode(breakpointData)
                "Time" ->
                    (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                        .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        } else {
            val item = deserializeLiveInstrumentRemoved(JsonObject(event.data))
            return when (name) {
                "Breakpoint Data" -> item.cause!!.message ?: item.cause!!.exceptionType
                "Time" -> (Clock.System.now().toEpochMilliseconds() - item.occurredAt.toEpochMilliseconds())
                    .toPrettyDuration() + " " + message("ago")
                else -> item.toString()
            }
        }
    }
}
