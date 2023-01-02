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

import com.intellij.util.ui.ColumnInfo
import spp.protocol.artifact.trace.Trace
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.Icon

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
            "Duration" -> Duration::class.java
            "Status" -> Icon::class.java
            else -> super.getColumnClass()
        }
    }

    override fun getComparator(): Comparator<Trace>? {
        return when (name) {
            "Time" -> Comparator { t: Trace, t2: Trace ->
                t.start.compareTo(t2.start)
            }

            "Duration" -> Comparator { t: Trace, t2: Trace ->
                t.duration.compareTo(t2.duration)
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
