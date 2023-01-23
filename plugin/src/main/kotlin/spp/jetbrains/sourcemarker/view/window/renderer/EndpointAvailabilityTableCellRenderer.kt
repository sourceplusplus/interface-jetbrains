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
package spp.jetbrains.sourcemarker.view.window.renderer

import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.DarculaColors
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.ListTableModel
import spp.jetbrains.PluginUI
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.sourcemarker.view.model.ServiceEndpointRow
import javax.swing.JTable

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EndpointAvailabilityTableCellRenderer : ColoredTableCellRenderer() {

    override fun customizeCellRenderer(
        table: JTable,
        value: Any?,
        selected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ) {
        val endpointRow = (table.model as ListTableModel<ServiceEndpointRow>)
            .getRowValue(table.rowSorter.convertRowIndexToModel(row))

        if (endpointRow.sla < 80.0) {
            append(
                endpointRow.sla.toString() + "%",
                SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, DarculaColors.RED)
            )
            icon = PluginIcons.errorBug
        } else if (endpointRow.sla < 90.0) {
            append(
                endpointRow.sla.toString() + "%",
                SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, PluginUI.yellow)
            )
            icon = PluginIcons.triangleExclamation
        } else {
            append(
                endpointRow.sla.toString() + "%",
                SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, PluginUI.green)
            )
            icon = PluginIcons.check
        }
        isTransparentIconBackground = true
    }
}
