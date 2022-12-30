package spp.jetbrains.sourcemarker.service.view.window

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ListTableModel
import io.vertx.kotlin.coroutines.await
import spp.jetbrains.UserData
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.service.view.trace.column.ServiceInstanceColumnInfo
import spp.protocol.platform.general.Service
import spp.protocol.platform.general.ServiceInstance
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SortOrder

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveInstancesWindow(project: Project, service: Service) : LiveViewChartWindow(project) {

    override val layoutComponent: JComponent
        get() = component

    val component: JPanel = JPanel(BorderLayout())
    val model: ListTableModel<ServiceInstance> = ListTableModel<ServiceInstance>(
        arrayOf(
            ServiceInstanceColumnInfo("Name")
        ),
        ArrayList(), 0, SortOrder.DESCENDING
    )

    init {
        val table = JBTable(model)
        table.isStriped = true
        table.setShowColumns(true)
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent) {
                val point: Point = mouseEvent.point
                val row = table.rowAtPoint(point)
                if (mouseEvent.clickCount == 2 && row >= 0) {
                    ApplicationManager.getApplication().invokeLater {
                        println("Todo")
                    }
                }
            }
        })
        component.add(JBScrollPane(table), "Center")

        val vertx = UserData.vertx(project)
//        ServiceBridge.currentServiceConsumer(vertx).handler {
//            val service = it.body()
            vertx.safeLaunch {
                UserData.liveManagementService(project)?.getInstances(service.id)?.await()?.forEach {
                    model.addRow(it)
                }
//            }
        }
    }
}