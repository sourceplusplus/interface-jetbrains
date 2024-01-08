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
package spp.jetbrains.instrument.renderer

import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.IconTableCellRenderer
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.instrument.model.InstrumentOverview
import spp.protocol.instrument.LiveInstrumentType
import spp.protocol.instrument.event.LiveInstrumentEvent
import javax.swing.Icon
import javax.swing.JTable

/**
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class InstrumentTypeTableCellRenderer : IconTableCellRenderer<String>() {

    override fun getIcon(value: String, table: JTable, row: Int): Icon {
        val event = (table.model as ListTableModel<*>).getRowValue(table.convertRowIndexToModel(row))

        if (event is LiveInstrumentEvent) {
            return when (event.instrument.type) {
                LiveInstrumentType.BREAKPOINT -> PluginIcons.breakpointConfig
                LiveInstrumentType.LOG -> PluginIcons.logConfig
                LiveInstrumentType.METER -> PluginIcons.meterConfig
                LiveInstrumentType.SPAN -> PluginIcons.spanConfig
            }
        } else {
            event as InstrumentOverview
            return when (event.instrumentType) {
                LiveInstrumentType.BREAKPOINT -> PluginIcons.breakpointConfig
                LiveInstrumentType.LOG -> PluginIcons.logConfig
                LiveInstrumentType.METER -> PluginIcons.meterConfig
                LiveInstrumentType.SPAN -> PluginIcons.spanConfig
            }
        }
    }
}
