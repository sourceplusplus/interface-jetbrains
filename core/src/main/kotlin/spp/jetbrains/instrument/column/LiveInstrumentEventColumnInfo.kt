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
package spp.jetbrains.instrument.column

import com.intellij.util.ui.ColumnInfo
import spp.jetbrains.instrument.renderer.InstrumentTypeTableCellRenderer
import spp.protocol.instrument.event.LiveBreakpointHit
import spp.protocol.instrument.event.LiveInstrumentEvent
import spp.protocol.instrument.event.LiveInstrumentRemoved
import spp.protocol.instrument.event.LiveLogHit
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * todo: description.
 *
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveInstrumentEventColumnInfo(name: String) : ColumnInfo<LiveInstrumentEvent, String>(name) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
        .withZone(ZoneId.systemDefault())

    override fun getColumnClass(): Class<*> {
        return when (name) {
            "Instrument Type" -> InstrumentTypeTableCellRenderer::class.java
            else -> super.getColumnClass()
        }
    }

    override fun getComparator(): Comparator<LiveInstrumentEvent>? {
        return when (name) {
            "Occurred At" -> Comparator.comparing { it.occurredAt }
            "Event Type" -> Comparator.comparing { it.eventType }
            "Data" -> Comparator { o1, o2 ->
                when {
                    o1 is LiveLogHit && o2 is LiveLogHit -> {
                        o1.logResult.logs.first().toFormattedMessage()
                            .compareTo(o2.logResult.logs.first().toFormattedMessage())
                    }

                    o1 is LiveBreakpointHit && o2 is LiveBreakpointHit -> {
                        o1.stackTrace.first().variables.joinToString(", ") { it.name + "=" + it.value }
                            .compareTo(o2.stackTrace.first().variables.joinToString(", ") { it.name + "=" + it.value })
                    }

                    o1 is LiveInstrumentRemoved && o2 is LiveInstrumentRemoved -> {
                        if (o1.cause != null && o2.cause != null) {
                            o1.cause.toString().compareTo(o2.cause.toString())
                        } else {
                            0
                        }
                    }

                    else -> 0
                }
            }

            else -> null
        }
    }

    override fun valueOf(item: LiveInstrumentEvent): String {
        return when (name) {
            "Occurred At" -> formatter.format(item.occurredAt)
            "Event Type" -> {
                item.eventType.name.substringAfter("_").lowercase().replaceFirstChar { it.titlecase() }
            }

            "Data" -> {
                if (item is LiveLogHit) {
                    item.logResult.logs.first().toFormattedMessage()
                } else if (item is LiveBreakpointHit) {
                    item.stackTrace.first().variables.joinToString(", ") { it.name + "=" + it.value }
                } else if (item is LiveInstrumentRemoved) {
                    if (item.cause != null) {
                        item.cause.toString()
                    } else {
                        ""
                    }
                } else {
                    ""
                }
            }

            else -> item.toString()
        }
    }
}
