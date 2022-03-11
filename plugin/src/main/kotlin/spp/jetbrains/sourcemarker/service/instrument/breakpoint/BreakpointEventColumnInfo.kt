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
package spp.jetbrains.sourcemarker.service.instrument.breakpoint

import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.table.IconTableCellRenderer
import kotlinx.datetime.toJavaInstant
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons
import spp.protocol.artifact.exception.methodName
import spp.protocol.artifact.exception.qualifiedClassName
import spp.protocol.artifact.exception.shortQualifiedClassName
import spp.protocol.artifact.exception.sourceAsLineNumber
import spp.protocol.instrument.event.LiveBreakpointHit
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.Icon
import javax.swing.table.TableCellRenderer

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class BreakpointEventColumnInfo(name: String) : ColumnInfo<LiveBreakpointHit, String>(name) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
        .withZone(ZoneId.systemDefault())

    override fun getColumnClass(): Class<*> {
        return when (name) {
            "Breakpoint Data" -> Icon::class.java
            else -> super.getColumnClass()
        }
    }

    override fun getCustomizedRenderer(o: LiveBreakpointHit, renderer: TableCellRenderer): TableCellRenderer {
        return when (name) {
            "Breakpoint Data" -> IconTableCellRenderer.create(SourceMarkerIcons.LIVE_BREAKPOINT_ACTIVE_ICON)
            else -> super.getCustomizedRenderer(o, renderer)
        }
    }

    override fun getComparator(): Comparator<LiveBreakpointHit>? {
        return when (name) {
            "Time" -> Comparator { t: LiveBreakpointHit, t2: LiveBreakpointHit ->
                t.occurredAt.compareTo(t2.occurredAt) //todo: could add line number too
            }
            "Class Name" -> Comparator { t: LiveBreakpointHit, t2: LiveBreakpointHit ->
                t.stackTrace.first().qualifiedClassName().compareTo(t2.stackTrace.first().qualifiedClassName())
            }
            "File Name" -> Comparator { t: LiveBreakpointHit, t2: LiveBreakpointHit ->
                t.stackTrace.first().source.compareTo(t2.stackTrace.first().source)
            }
            "Line No" -> Comparator { t: LiveBreakpointHit, t2: LiveBreakpointHit ->
                t.stackTrace.first().sourceAsLineNumber()!!.compareTo(t2.stackTrace.first().sourceAsLineNumber()!!)
            }
            else -> null
        }
    }

    override fun valueOf(item: LiveBreakpointHit): String {
        return when (name) {
            "Time" -> formatter.format(item.occurredAt.toJavaInstant())
            "Host Name" -> item.serviceInstance.substringAfter("@")
            "Service" -> item.service
            "Class Name" -> item.stackTrace.first().shortQualifiedClassName()
            "File Name" -> item.stackTrace.first().source
            "Method Name" -> item.stackTrace.first().methodName()
            "Line No" -> item.stackTrace.first().sourceAsLineNumber()!!.toString()
            "Breakpoint Data" -> "View Frames/Variables"
            else -> item.toString()
        }
    }
}
