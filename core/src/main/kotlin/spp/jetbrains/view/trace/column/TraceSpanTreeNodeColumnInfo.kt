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
package spp.jetbrains.view.trace.column

import com.intellij.util.ui.ColumnInfo
import spp.jetbrains.view.trace.node.TraceSpanTreeNode
import spp.protocol.utils.toPrettyDuration
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceSpanTreeNodeColumnInfo(name: String) : ColumnInfo<TraceSpanTreeNode, String>(name) {

    private val formatter = DateTimeFormatter.ofPattern("h:mm:ss.SSS a")
        .withZone(ZoneId.systemDefault())

    override fun getColumnClass(): Class<*> {
        return when (name) {
            "Duration" -> Duration::class.java
            else -> super.getColumnClass()
        }
    }

    override fun getComparator(): Comparator<TraceSpanTreeNode> {
        return when (name) {
            "Time" -> Comparator { t: TraceSpanTreeNode, t2: TraceSpanTreeNode ->
                t.value.startTime.compareTo(t2.value.startTime)
            }

            "Duration" -> Comparator { t: TraceSpanTreeNode, t2: TraceSpanTreeNode ->
                (t.value.endTime.toEpochMilli() - t.value.startTime.toEpochMilli())
                    .compareTo(t2.value.endTime.toEpochMilli() - t2.value.startTime.toEpochMilli())
            }

            else -> Comparator { t: TraceSpanTreeNode, t2: TraceSpanTreeNode ->
                t.toString().compareTo(t2.toString())
            }
        }
    }

    override fun valueOf(item: TraceSpanTreeNode): String {
        return when (name) {
            "Trace" -> item.value.endpointName.toString()
            "Time" -> formatter.format(item.value.startTime)
            "Type" -> item.value.type
            "Duration" -> (item.value.endTime.toEpochMilli() - item.value.startTime.toEpochMilli()).toPrettyDuration()
            else -> ""
        }
    }
}
