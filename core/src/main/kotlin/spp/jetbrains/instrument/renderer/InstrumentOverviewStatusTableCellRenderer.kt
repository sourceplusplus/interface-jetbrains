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
package spp.jetbrains.instrument.renderer

import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.IconTableCellRenderer
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.instrument.model.InstrumentOverview
import javax.swing.Icon
import javax.swing.JTable

/**
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class InstrumentOverviewStatusTableCellRenderer : IconTableCellRenderer<String>() {

    override fun getIcon(value: String, table: JTable, row: Int): Icon {
        val event = (table.model as ListTableModel<InstrumentOverview>).getRowValue(table.convertRowIndexToModel(row))
        return when (event.status) {
            "Error" -> PluginIcons.Instrument.Overview.error
            "Active" -> PluginIcons.Instrument.Overview.active
            "Pending" -> PluginIcons.Instrument.Overview.pending
            else -> PluginIcons.Instrument.Overview.complete
        }
    }
}
