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

import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.ListTableModel
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.instrument.model.InstrumentOverview
import javax.swing.JTable

/**
 * todo: description.
 *
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class CreatedByTableCellRenderer(val project: Project) : ColoredTableCellRenderer() {

    override fun customizeCellRenderer(
        table: JTable,
        value: Any?,
        selected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ) {
        val instrumentOverview = (table.model as ListTableModel<InstrumentOverview>)
            .getRowValue(table.convertRowIndexToModel(row))

        if (instrumentOverview.createdBy == UserData.selfInfo(project)?.developer?.id) {
            append(
                instrumentOverview.createdBy,
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            )
            icon = PluginIcons.user
            isTransparentIconBackground = true
        } else {
            append(
                instrumentOverview.createdBy,
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            )
        }
    }
}
