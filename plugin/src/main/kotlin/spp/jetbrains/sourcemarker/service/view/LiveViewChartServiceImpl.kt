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
package spp.jetbrains.sourcemarker.service.view

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.plugin.LiveViewChartService
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.service.view.action.ChangeChartAction
import spp.jetbrains.sourcemarker.service.view.action.ChangeTimeAction
import spp.jetbrains.sourcemarker.service.view.window.LiveActivityWindow
import spp.jetbrains.sourcemarker.service.view.window.LiveEndpointsWindow
import spp.jetbrains.sourcemarker.service.view.window.LiveOverviewWindow
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatusListener
import spp.protocol.platform.general.Service

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewChartServiceImpl(private val project: Project) : LiveViewChartService, Disposable {

    companion object {
        fun init(project: Project) {
            project.getService(LiveViewChartServiceImpl::class.java)
        }
    }

    private var toolWindow = ToolWindowManager.getInstance(project)
        .registerToolWindow(RegisterToolWindowTask.closable("Live Activity", PluginIcons.chartArea))
    private var contentManager = toolWindow.contentManager

    init {
        project.putUserData(LiveViewChartService.KEY, this)
        project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
            if (it == SourceStatus.Ready) {
                val vertx = UserData.vertx(project)
                vertx.safeLaunch {
                    val service = ServiceBridge.getCurrentService(vertx)!!
                    ApplicationManager.getApplication().invokeLater {
                        showLiveViewChartsWindow(service)
                    }
                }
            } else {
                ApplicationManager.getApplication().invokeLater {
                    hideLiveViewChartsWindow()
                }
            }
        })

        Disposer.register(this, contentManager)
    }

    private fun showLiveViewChartsWindow(service: Service) {
        toolWindow.setTitleActions(
            listOf(
                ChangeChartAction(),
                ChangeTimeAction()
            )
        )

        val chartsWindow = LiveOverviewWindow(project, service)
        val overviewContent = ContentFactory.getInstance().createContent(
            chartsWindow.layoutComponent,
            "Overview",
            true
        )
        overviewContent.setDisposer(chartsWindow)
        overviewContent.isCloseable = false
        contentManager.addContent(overviewContent)

//        val instancesWindow = LiveInstancesWindow(project, service)
//        val instancesContent = ContentFactory.getInstance().createContent(
//            instancesWindow.layoutComponent,
//            "Instances",
//            true
//        )
//        instancesContent.setDisposer(instancesWindow)
//        instancesContent.isCloseable = false
//        contentManager.addContent(instancesContent)

        val endpointsWindow = LiveEndpointsWindow(project, service)
        val endpointsContent = ContentFactory.getInstance().createContent(
            endpointsWindow.layoutComponent,
            "Endpoints",
            true
        )
        endpointsContent.setDisposer(endpointsWindow)
        endpointsContent.isCloseable = false
        contentManager.addContent(endpointsContent)
    }

    private fun hideLiveViewChartsWindow() {
        toolWindow.setTitleActions(emptyList())
        contentManager.contents.forEach { content ->
            contentManager.removeContent(content, true)
        }
    }

    override fun showOverview() = ApplicationManager.getApplication().invokeLater {
        contentManager.setSelectedContent(contentManager.findContent("Overview"))
        toolWindow.show()
    }

    override fun doThing(endpointName: String) = ApplicationManager.getApplication().invokeLater {
        val activityWindow = LiveActivityWindow(project, endpointName)
        val content = ContentFactory.getInstance().createContent(
            activityWindow.layoutComponent,
            endpointName,
            false
        )
        content.setDisposer(activityWindow)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        toolWindow.show()
    }

    override fun dispose() = Unit
}
