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
package spp.jetbrains.sourcemarker.view.trace.column

import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.table.IconTableCellRenderer
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.sourcemarker.view.trace.node.SpanInfoListNode
import spp.jetbrains.sourcemarker.view.trace.node.TraceListNode
import spp.protocol.artifact.trace.Trace
import spp.protocol.artifact.trace.TraceSpan
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.Icon
import javax.swing.table.TableCellRenderer

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceColumnInfo(name: String) : ColumnInfo<NodeDescriptor<*>, String>(name) {

    private val formatter = DateTimeFormatter.ofPattern("h:mm:ss a")
        .withZone(ZoneId.systemDefault())

    override fun getColumnClass(): Class<*> {
        return when (name) {
            "Duration" -> Icon::class.java
            else -> super.getColumnClass()
        }
    }

    override fun getCustomizedRenderer(o: NodeDescriptor<*>, renderer: TableCellRenderer): TableCellRenderer {
        return when (name) {
            "Breakpoint Data" -> IconTableCellRenderer.create(PluginIcons.Breakpoint.active)
            else -> super.getCustomizedRenderer(o, renderer)
        }
    }

    override fun getComparator(): Comparator<NodeDescriptor<*>>? {
        return when (name) {
            "Time" -> Comparator { t: NodeDescriptor<*>, t2: NodeDescriptor<*> ->
                if (t is TraceListNode) {
                    t2 as TraceListNode
                    (t.value as Trace).start.compareTo((t2.value as Trace).start)
                } else if (t is SpanInfoListNode) {
                    t2 as SpanInfoListNode
                    (t.value as TraceSpan).startTime.compareTo((t2.value as TraceSpan).startTime)
                } else {
                    t.toString().compareTo(t2.toString())
                }
            }

            "Duration" -> Comparator { t: NodeDescriptor<*>, t2: NodeDescriptor<*> ->
                if (t is TraceListNode) {
                    t2 as TraceListNode
                    (t.value as Trace).duration.compareTo((t2.value as Trace).duration)
                } else if (t is SpanInfoListNode) {
                    t2 as SpanInfoListNode
                    (t.value as TraceSpan).startTime.compareTo((t2.value as TraceSpan).startTime)
                } else {
                    t.toString().compareTo(t2.toString())
                }
            }

            else -> null
        }
    }

    override fun valueOf(item: NodeDescriptor<*>): String {
        if (item is TraceListNode) {
            return when (name) {
                "Time" -> formatter.format((item.value as Trace).start)
                "Duration" -> "${(item.value as Trace).duration} ms"
                else -> item.toString()
            }
        }
        return when (name) {
            else -> item.toString()
        }
    }
}
