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
package spp.jetbrains.sourcemarker.view.trace.table

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import org.joor.Reflect
import spp.jetbrains.view.trace.column.PairColumnInfo
import spp.protocol.artifact.trace.TraceSpan
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TraceSpanTable(span: TraceSpan) : Disposable {

    private val model = ListTableModel<Pair<String, Any?>>(
        PairColumnInfo("Key", true),
        PairColumnInfo("Value", false),
    )
    val component: JPanel = JPanel(BorderLayout())

    init {
        val table = JBTable(model)
        table.setShowColumns(true)
        component.add(JBScrollPane(table), "Center")

        setSpan(span)
    }

    fun setSpan(span: TraceSpan) {
        repeat(model.rowCount) {
            model.removeRow(0)
        }
        Reflect.on(span).fields().map { field ->
            val value = field.value.get<Any?>()
            model.addRow(field.key to value)
        }.toMutableList()
    }

    override fun dispose() = Unit
}
