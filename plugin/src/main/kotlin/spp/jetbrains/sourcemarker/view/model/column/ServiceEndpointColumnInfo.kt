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
package spp.jetbrains.sourcemarker.view.model.column

import com.intellij.util.ui.ColumnInfo
import spp.jetbrains.sourcemarker.view.model.ServiceEndpointRow
import spp.protocol.utils.fromPerSecondToPrettyFrequency
import spp.protocol.utils.toPrettyDuration
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ServiceEndpointColumnInfo(name: String) : ColumnInfo<ServiceEndpointRow, String>(name) {

    override fun getColumnClass(): Class<*> {
        return when (name) {
            "Availability" -> Icon::class.java
            else -> String::class.java
        }
    }

    override fun getComparator(): Comparator<ServiceEndpointRow>? {
        return when (name) {
            "Name" -> Comparator { t: ServiceEndpointRow, t2: ServiceEndpointRow ->
                t.endpoint.name.compareTo(t2.endpoint.name)
            }

            "Latency" -> Comparator { t: ServiceEndpointRow, t2: ServiceEndpointRow ->
                t.respTimeAvg.compareTo(t2.respTimeAvg)
            }

            "Availability" -> Comparator { t: ServiceEndpointRow, t2: ServiceEndpointRow ->
                t.sla.compareTo(t2.sla)
            }

            "Throughput" -> Comparator { t: ServiceEndpointRow, t2: ServiceEndpointRow ->
                t.cpm.compareTo(t2.cpm)
            }

            else -> null
        }
    }

    override fun valueOf(item: ServiceEndpointRow): String {
        return when (name) {
            "Name" -> item.endpoint.name
            "Latency" -> item.respTimeAvg.toPrettyDuration()
            "Availability" -> item.sla.toString() + "%"
            "Throughput" -> item.cpm.toDouble().fromPerSecondToPrettyFrequency()
            else -> item.toString()
        }
    }
}
