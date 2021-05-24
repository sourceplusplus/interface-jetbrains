package com.sourceplusplus.sourcemarker.service.breakpoint.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.IconTableCellRenderer
import com.sourceplusplus.protocol.instrument.breakpoint.event.LiveBreakpointHit
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointHitColumnInfo
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointHitWindowService
import com.sourceplusplus.sourcemarker.icons.SourceMarkerIcons
import java.awt.BorderLayout
import java.awt.Point
import javax.swing.*

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class EventsTab : Disposable {

    val component: JPanel = JPanel(BorderLayout())
    val model: ListTableModel<LiveBreakpointHit> = ListTableModel<LiveBreakpointHit>(
        arrayOf(
            BreakpointHitColumnInfo("Time"),
            BreakpointHitColumnInfo("Host Name"),
            BreakpointHitColumnInfo("Application Name"),
            BreakpointHitColumnInfo("Class Name"),
            BreakpointHitColumnInfo("Method Name"),
            BreakpointHitColumnInfo("Line No"),
            BreakpointHitColumnInfo("Hindsight Data")
        ),
        ArrayList(), 0, SortOrder.DESCENDING
    )

    init {
        val table = JBTable(model)
        table.isStriped = true
        table.setShowColumns(true)
        table.setDefaultRenderer(Icon::class.java, IconTableCellRenderer.create(SourceMarkerIcons.EYE_ICON))
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

    override fun dispose() {}
}
