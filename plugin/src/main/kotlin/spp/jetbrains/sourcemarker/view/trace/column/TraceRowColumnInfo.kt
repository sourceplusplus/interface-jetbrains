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
package spp.jetbrains.sourcemarker.view.trace.column

import com.intellij.util.ui.ColumnInfo
import spp.jetbrains.view.trace.renderer.TraceDurationTableCellRenderer
import spp.jetbrains.view.trace.renderer.TraceErrorTableCellRenderer
import spp.protocol.artifact.trace.Trace
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceRowColumnInfo(name: String) : ColumnInfo<Trace, String>(name) {

    private val formatter = DateTimeFormatter.ofPattern("h:mm:ss.SSS a")
        .withZone(ZoneId.systemDefault())

    override fun getColumnClass(): Class<*> {
        return when (name) {
            "Duration" -> TraceDurationTableCellRenderer::class.java
            "Status" -> TraceErrorTableCellRenderer::class.java
            else -> super.getColumnClass()
        }
    }

    override fun getComparator(): Comparator<Trace>? {
        return when (name) {
            "Trace" -> Comparator { t, t2 -> t.operationNames.first().compareTo(t2.operationNames.first()) }
            "Time" -> Comparator { t, t2 -> t.start.compareTo(t2.start) }
            "Duration" -> Comparator { t, t2 -> t.duration.compareTo(t2.duration) }
            "Status" -> Comparator { t, t2 ->
                if (t.error == t2.error) {
                    val tStatusCode = t.meta["http.status_code"] ?: "KO"
                    val t2StatusCode = t2.meta["http.status_code"] ?: "KO"
                    tStatusCode.compareTo(t2StatusCode)
                } else {
                    t.error.toString().compareTo(t2.error.toString())
                }
            }

            else -> null
        }
    }

    override fun valueOf(item: Trace): String {
        return when (name) {
            "Trace" -> item.operationNames.first()
            "Time" -> formatter.format(item.start)
            "Duration" -> "${item.duration}"
            "Status" -> item.error.toString()
            else -> item.toString()
        }
    }
}
