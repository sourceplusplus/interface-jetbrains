/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.service.breakpoint.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.IconTableCellRenderer
import spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons.LIVE_BREAKPOINT_ACTIVE_ICON
import spp.jetbrains.sourcemarker.service.breakpoint.BreakpointEventColumnInfo
import spp.jetbrains.sourcemarker.service.breakpoint.BreakpointHitWindowService
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
class EventsTab : Disposable {

    val component: JPanel = JPanel(BorderLayout())
    val model: ListTableModel<LiveBreakpointHit> = ListTableModel<LiveBreakpointHit>(
        arrayOf(
            BreakpointEventColumnInfo("Time"),
            BreakpointEventColumnInfo("Host Name"),
            BreakpointEventColumnInfo("Service"),
            let {
                val productCode = ApplicationInfo.getInstance().build.productCode
                if (PluginSourceMarkerStartupActivity.PYCHARM_PRODUCT_CODES.contains(productCode)) {
                    BreakpointEventColumnInfo("File Name")
                } else {
                    BreakpointEventColumnInfo("Class Name")
                }
            },
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
        table.setDefaultRenderer(Icon::class.java, IconTableCellRenderer.create(LIVE_BREAKPOINT_ACTIVE_ICON))
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val point: Point = mouseEvent.point
                val row = table.rowAtPoint(point)
                if (mouseEvent.clickCount == 2 && row >= 0) {
                    ApplicationManager.getApplication().invokeLater {
                        val project = ProjectManager.getInstance().openProjects[0]
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
