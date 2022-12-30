package spp.jetbrains.sourcemarker.service.view.window

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import spp.jetbrains.UserData
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_CPM
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_RespTime_AVG
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_SLA
import javax.swing.BoxLayout
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveActivityWindow(project: Project, endpointName: String) : LiveViewChartWindow(project) {

    init {
        val vertx = UserData.vertx(project)

        val respTimePercentilesChart = setupRespTimePercentileChart(endpointName, vertx)
        val respTimePercentilesPanel = JPanel()
        respTimePercentilesPanel.layout = BoxLayout(respTimePercentilesPanel, BoxLayout.Y_AXIS)
        respTimePercentilesPanel.add(respTimePercentilesChart.component)

        val respTimeChart = setupSingleLineChart(endpointName, vertx, Endpoint_RespTime_AVG.asRealtime())
        val respTimePanel = JPanel()
        respTimePanel.layout = BoxLayout(respTimePanel, BoxLayout.Y_AXIS)
        respTimePanel.add(respTimeChart.component)

        val slaChart = setupSingleLineChart(endpointName, vertx, Endpoint_SLA.asRealtime())
        val slaPanel = JPanel()
        slaPanel.layout = BoxLayout(slaPanel, BoxLayout.Y_AXIS)
        slaPanel.add(slaChart.component)

        val loadChart = setupSingleLineChart(endpointName, vertx, Endpoint_CPM.asRealtime())
        val loadPanel = JPanel()
        loadPanel.layout = BoxLayout(loadPanel, BoxLayout.Y_AXIS)
        loadPanel.add(loadChart.component)

        var index = 0
        tabbedPane.tabComponentInsets = JBUI.emptyInsets()
        tabbedPane.insertTab("Response Percentile (ms)", null, respTimePercentilesPanel, null, index++)
        tabbedPane.insertTab("Response Average (ms)", null, respTimePanel, null, index++)
        tabbedPane.insertTab("SLA (%)", null, slaPanel, null, index++)
        tabbedPane.insertTab("Load (calls/min)", null, loadPanel, null, index++)
    }
}
