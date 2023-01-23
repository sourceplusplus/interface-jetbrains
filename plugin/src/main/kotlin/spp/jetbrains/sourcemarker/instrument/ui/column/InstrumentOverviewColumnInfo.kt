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
package spp.jetbrains.sourcemarker.instrument.ui.column

import com.intellij.util.ui.ColumnInfo
import spp.jetbrains.sourcemarker.instrument.ui.model.InstrumentOverview
import spp.jetbrains.sourcemarker.instrument.ui.renderer.CreatedByTableCellRenderer
import spp.jetbrains.sourcemarker.instrument.ui.renderer.InstrumentOverviewStatusTableCellRenderer
import spp.jetbrains.sourcemarker.instrument.ui.renderer.InstrumentTypeTableCellRenderer
import spp.protocol.utils.toPrettyDuration
import java.time.Instant

/**
 * todo: description.
 *
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class InstrumentOverviewColumnInfo(name: String) : ColumnInfo<InstrumentOverview, String>(name) {

    override fun getColumnClass(): Class<*> {
        return when (name) {
            "Status" -> InstrumentOverviewStatusTableCellRenderer::class.java
            "Instrument Type" -> InstrumentTypeTableCellRenderer::class.java
            "Created By" -> CreatedByTableCellRenderer::class.java
            else -> super.getColumnClass()
        }
    }

    override fun getComparator(): Comparator<InstrumentOverview>? {
        return when (name) {
            "First Event" -> Comparator { o1, o2 ->
                o1.firstEvent.compareTo(o2.firstEvent)
            }

            "Last Event" -> Comparator { o1, o2 ->
                o1.lastEvent.compareTo(o2.lastEvent)
            }

            "Event Count" -> Comparator { o1, o2 ->
                o1.events.size.compareTo(o2.events.size)
            }

            "Created By" -> Comparator { o1, o2 ->
                o1.createdBy.compareTo(o2.createdBy)
            }

            "Source" -> Comparator { o1, o2 ->
                o1.source.compareTo(o2.source)
            }

            "Instrument Type" -> Comparator { o1, o2 ->
                o1.instrumentTypeFormatted.compareTo(o2.instrumentTypeFormatted)
            }

            "Status" -> Comparator { o1, o2 ->
                o1.status.compareTo(o2.status)
            }

            else -> null
        }
    }

    override fun valueOf(item: InstrumentOverview): String {
        return when (name) {
            "First Event" -> toPrettyElapsed(item.firstEvent)
            "Last Event" -> toPrettyElapsed(item.lastEvent)
            "Event Count" -> item.events.size.toString()
            "Source" -> item.source
            "Status" -> item.status
            "Created By" -> item.createdBy
            "Instrument Type" -> item.instrumentTypeFormatted
            else -> item.toString()
        }
    }

    private fun toPrettyElapsed(item: Instant): String {
        val elapsedTime = System.currentTimeMillis() - item.toEpochMilli()
        if (elapsedTime < 2000) {
            return "moments ago"
        }
        return elapsedTime.toPrettyDuration(pluralize = false) + " ago"
    }
}
