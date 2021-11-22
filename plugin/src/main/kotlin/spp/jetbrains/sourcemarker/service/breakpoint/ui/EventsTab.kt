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
import spp.protocol.instrument.breakpoint.event.LiveBreakpointHit
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

    override fun dispose() {}
}
