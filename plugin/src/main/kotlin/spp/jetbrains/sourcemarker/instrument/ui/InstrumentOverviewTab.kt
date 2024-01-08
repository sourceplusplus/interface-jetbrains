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
package spp.jetbrains.sourcemarker.instrument.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import spp.jetbrains.instrument.column.InstrumentOverviewColumnInfo
import spp.jetbrains.instrument.model.InstrumentOverview
import spp.jetbrains.instrument.renderer.CreatedByTableCellRenderer
import spp.jetbrains.instrument.renderer.InstrumentOverviewStatusTableCellRenderer
import spp.jetbrains.instrument.renderer.InstrumentTypeTableCellRenderer
import spp.jetbrains.invokeLater
import spp.jetbrains.sourcemarker.instrument.InstrumentEventWindowService
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * todo: description.
 *
 * @since 0.7.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class InstrumentOverviewTab(val project: Project) : Disposable {

    val model = ListTableModel<InstrumentOverview>(
        arrayOf(
            InstrumentOverviewColumnInfo("First Event"),
            InstrumentOverviewColumnInfo("Last Event"),
            InstrumentOverviewColumnInfo("Event Count"),
            InstrumentOverviewColumnInfo("Source"),
            InstrumentOverviewColumnInfo("Created By"),
            InstrumentOverviewColumnInfo("Instrument Type"),
            InstrumentOverviewColumnInfo("Status")
        ),
        ArrayList(), 0, SortOrder.DESCENDING
    )
    val table = JBTable(model)
    val component = JPanel(BorderLayout()).apply {
        add(JBScrollPane(table), BorderLayout.CENTER)
    }
    val selectedInstrumentOverview: InstrumentOverview?
        get() {
            val selectedRow = table.selectedRow
            if (selectedRow == -1) return null
            return model.getItem(table.convertRowIndexToModel(selectedRow))
        }

    init {
        table.setShowColumns(true)
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.setDefaultRenderer(
            InstrumentOverviewStatusTableCellRenderer::class.java,
            InstrumentOverviewStatusTableCellRenderer()
        )
        table.setDefaultRenderer(
            InstrumentTypeTableCellRenderer::class.java,
            InstrumentTypeTableCellRenderer()
        )
        table.setDefaultRenderer(
            CreatedByTableCellRenderer::class.java,
            CreatedByTableCellRenderer(project)
        )
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val point = mouseEvent.point
                val row = table.rowAtPoint(point)
                if (mouseEvent.clickCount == 2 && row >= 0) {
                    project.invokeLater {
                        InstrumentEventWindowService.getInstance(project)
                            .showInstrumentEvents(model.getItem(table.convertRowIndexToModel(row)))
                    }
                }
            }
        })
        table.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter")
        table.actionMap.put("enter", object : AbstractAction() {
            override fun actionPerformed(actionEvent: ActionEvent) {
                project.invokeLater {
                    InstrumentEventWindowService.getInstance(project)
                        .showInstrumentEvents(model.getItem(table.convertRowIndexToModel(table.selectedRow)))
                }
            }
        })

        //todo: ensure this is best thread for job
        //repaint every second
        ApplicationManager.getApplication().executeOnPooledThread {
            while (true) {
                Thread.sleep(1000)
                project.invokeLater {
                    table.repaint()
                }
            }
        }
    }

    override fun dispose() = Unit
}
