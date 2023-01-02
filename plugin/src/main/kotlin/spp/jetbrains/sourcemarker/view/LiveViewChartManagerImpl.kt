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
package spp.jetbrains.sourcemarker.view

import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.view.action.*
import spp.jetbrains.sourcemarker.view.window.LiveActivityWindow
import spp.jetbrains.sourcemarker.view.window.LiveEndpointsWindow
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatusListener
import spp.jetbrains.view.LiveViewChartManager
import spp.jetbrains.view.ResumableView
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.platform.general.Service

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewChartManagerImpl(
    private val project: Project
) : LiveViewChartManager, ContentManagerListener {

    companion object {
        fun init(project: Project) {
            project.getService(LiveViewChartManagerImpl::class.java)
        }
    }

    private var toolWindow = ToolWindowManager.getInstance(project)
        .registerToolWindow(RegisterToolWindowTask.closable("Live Activity", PluginIcons.chartArea))
    private var contentManager = toolWindow.contentManager
    override var currentView: ResumableView? = null

    init {
        project.putUserData(LiveViewChartManager.KEY, this)
        project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
            if (it == SourceStatus.Ready) {
                val vertx = UserData.vertx(project)
                vertx.safeLaunch {
                    val service = ServiceBridge.getCurrentService(vertx)!!
                    ApplicationManager.getApplication().invokeLater {
                        showWindow(service)
                    }
                }
            } else {
                ApplicationManager.getApplication().invokeLater {
                    hideWindow()
                }
            }
        })
        contentManager.addContentManagerListener(this)

        project.messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager, changeType: ToolWindowManagerEventType) {
                if (toolWindow.isVisible) {
                    (contentManager.contents.first().disposer as ResumableView).onFocused()
                } else {
                    //pause views when tool window is hidden
                    contentManager.contents
                        .mapNotNull { it.disposer as? ResumableView }
                        .forEach { it.pause() }
                }
            }
        })

        Disposer.register(this, contentManager)

        toolWindow.setTitleActions(
            listOf(
                ResumeViewAction(this),
                StopViewAction(this),
                SetRealtimeAction(this),
                Separator.getInstance(),
                ChangeChartAction(),
                ChangeTimeAction()
            )
        )
    }

    override fun selectionChanged(event: ContentManagerEvent) {
        if (event.operation == ContentManagerEvent.ContentOperation.add) {
            currentView = event.content.disposer as ResumableView

            if (toolWindow.isVisible) {
                currentView?.onFocused()
            }
        }
    }

    override fun contentRemoved(event: ContentManagerEvent) {
        val removedWindow = event.content.disposer as ResumableView
        removedWindow.pause()

        if (removedWindow == currentView) {
            currentView = null
        }
    }

    private fun showWindow(service: Service) {
        val overviewWindow = LiveActivityWindow(
            project, service.name, "Service", listOf(
                MetricType.Service_RespTime_AVG,
                MetricType.Service_SLA,
                MetricType.Service_CPM
            )
        )
        val overviewContent = ContentFactory.getInstance().createContent(
            overviewWindow.component,
            "Overview",
            true
        )
        overviewContent.setDisposer(overviewWindow)
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
            endpointsWindow.component,
            "Endpoints",
            true
        )
        endpointsContent.setDisposer(endpointsWindow)
        endpointsContent.isCloseable = false
        contentManager.addContent(endpointsContent)
    }

    private fun hideWindow() {
        contentManager.contents.forEach { content ->
            contentManager.removeContent(content, true)
        }
    }

    override fun showOverviewActivity() = ApplicationManager.getApplication().invokeLater {
        contentManager.setSelectedContent(contentManager.findContent("Overview"))
        toolWindow.show()
    }

    override fun showEndpointActivity(endpointName: String) = ApplicationManager.getApplication().invokeLater {
        val existingContent = contentManager.findContent(endpointName)
        if (existingContent != null) {
            contentManager.setSelectedContent(existingContent)
            toolWindow.show()
            return@invokeLater
        }

        val activityWindow = LiveActivityWindow(
            project, endpointName, "Endpoint", listOf(
                MetricType.Endpoint_RespTime_AVG.asRealtime(),
                MetricType.Endpoint_SLA.asRealtime(),
                MetricType.Endpoint_CPM.asRealtime()
            ), -1
        )
        activityWindow.resume()

        val content = ContentFactory.getInstance().createContent(
            activityWindow.component,
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
