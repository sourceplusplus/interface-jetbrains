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
package spp.jetbrains.sourcemarker.service.view.window

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import spp.jetbrains.UserData
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.artifact.metrics.MetricType.Companion.Service_RespTime_AVG
import spp.protocol.artifact.metrics.MetricType.Companion.Service_RespTime_Percentiles
import spp.protocol.platform.general.Service
import javax.swing.BoxLayout
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveOverviewWindow(project: Project, service: Service) : LiveViewChartWindow(project) {

    init {
        val vertx = UserData.vertx(project)

        val respTimeChart = setupSingleLineChart(service.name, vertx, Service_RespTime_AVG)
        val respTimePanel = JPanel()
        respTimePanel.layout = BoxLayout(respTimePanel, BoxLayout.Y_AXIS)
        respTimePanel.add(respTimeChart.component)

        val respTimePercentilesChart = setupRespTimePercentileChart(service.name, vertx, Service_RespTime_Percentiles)
        val respTimePercentilesPanel = JPanel()
        respTimePercentilesPanel.layout = BoxLayout(respTimePercentilesPanel, BoxLayout.Y_AXIS)
        respTimePercentilesPanel.add(respTimePercentilesChart.component)

        val slaChart = setupSingleLineChart(
            service.name, vertx, MetricType.Service_SLA
        )
        val slaPanel = JPanel()
        slaPanel.layout = BoxLayout(slaPanel, BoxLayout.Y_AXIS)
        slaPanel.add(slaChart.component)

        val loadChart = setupSingleLineChart(
            service.name, vertx, MetricType.Service_CPM
        )
        val loadPanel = JPanel()
        loadPanel.layout = BoxLayout(loadPanel, BoxLayout.Y_AXIS)
        loadPanel.add(loadChart.component)

        var index = 0
        tabbedPane.tabComponentInsets = JBUI.emptyInsets()
        tabbedPane.insertTab("Service Avg Response Time (ms)", null, respTimePanel, null, index++)
//        tabbedPane.insertTab("Service Apdex", null, JPanel(), null, 1)
        tabbedPane.insertTab("Service Response Time Percentile (ms)", null, respTimePercentilesPanel, null, index++)
        tabbedPane.insertTab("Service Load (calls/min)", null, loadPanel, null, index++)
        tabbedPane.insertTab("Success Rate (%)", null, slaPanel, null, index++)
    }
}
