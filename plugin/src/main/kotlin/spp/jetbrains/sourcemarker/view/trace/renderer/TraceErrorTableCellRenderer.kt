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
package spp.jetbrains.sourcemarker.view.trace.renderer

import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.IconTableCellRenderer
import spp.jetbrains.icons.PluginIcons
import spp.protocol.artifact.trace.Trace
import javax.swing.Icon
import javax.swing.JTable

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceErrorTableCellRenderer : IconTableCellRenderer<String>() {

    override fun getIcon(value: String, table: JTable, row: Int): Icon {
        val error = value.toBooleanStrict()
        val trace = (table.model as ListTableModel<Trace>).getRowValue(table.rowSorter.convertRowIndexToModel(row))
        val statusCode = trace.meta["http.status_code"] ?: if (error) "KO" else "OK"
        text = statusCode

        return if (error) {
            PluginIcons.errorBug
        } else {
            PluginIcons.check
        }
    }
}
