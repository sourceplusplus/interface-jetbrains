/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.instrument.breakpoint.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.IconTableCellRenderer
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.sourcemarker.instrument.breakpoint.BreakpointEventColumnInfo
import spp.jetbrains.sourcemarker.instrument.breakpoint.BreakpointHitWindowService
import spp.protocol.instrument.event.LiveBreakpointHit
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SortOrder

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EventsTab(val project: Project) : Disposable {

    val component: JPanel = JPanel(BorderLayout())
    val model: ListTableModel<LiveBreakpointHit> = ListTableModel<LiveBreakpointHit>(
        arrayOf(
            BreakpointEventColumnInfo("Time"),
            BreakpointEventColumnInfo("Host Name"),
            BreakpointEventColumnInfo("Service"),
            BreakpointEventColumnInfo("Class/File Name"),
            BreakpointEventColumnInfo("Method Name"),
            BreakpointEventColumnInfo("Line No"),
            BreakpointEventColumnInfo("Breakpoint Data")
        ),
        ArrayList(), 0, SortOrder.DESCENDING
    )

    init {
        val table = JBTable(model)
        table.isStriped = true
        table.setShowColumns(true)
        table.setDefaultRenderer(Icon::class.java, IconTableCellRenderer.create(PluginIcons.Breakpoint.active))
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val point: Point = mouseEvent.point
                val row = table.rowAtPoint(point)
                if (mouseEvent.clickCount == 2 && row >= 0) {
                    ApplicationManager.getApplication().invokeLater {
                        BreakpointHitWindowService.getInstance(project)
                            .showBreakpointHit(model.getItem(table.convertRowIndexToModel(row)))
                    }
                }
            }
        })
        component.add(JBScrollPane(table), "Center")
    }

    override fun dispose() = Unit
}
