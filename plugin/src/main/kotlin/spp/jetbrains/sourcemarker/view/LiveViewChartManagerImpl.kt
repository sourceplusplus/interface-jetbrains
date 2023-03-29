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
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.view.action.ChangeTimeAction
import spp.jetbrains.sourcemarker.view.action.ResumeViewAction
import spp.jetbrains.sourcemarker.view.action.SetRefreshIntervalAction
import spp.jetbrains.sourcemarker.view.action.StopViewAction
import spp.jetbrains.sourcemarker.view.window.LiveActivityWindow
import spp.jetbrains.sourcemarker.view.window.LiveEndpointsWindow
import spp.jetbrains.status.SourceStatusService
import spp.jetbrains.view.LiveViewChartManager
import spp.jetbrains.view.ResumableView
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.platform.general.Service
import spp.protocol.platform.general.ServiceEndpoint

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

    private val contentFactory = ApplicationManager.getApplication().getService(ContentFactory::class.java)
    private var toolWindow = ToolWindowManager.getInstance(project)
        .registerToolWindow(RegisterToolWindowTask.closable("Live Activity", PluginIcons.ToolWindow.chartArea))
    private var contentManager = toolWindow.contentManager
    override var currentView: ResumableView? = null
    override val refreshInterval: Int?
        get() = currentView?.refreshInterval

    init {
        project.putUserData(LiveViewChartManager.KEY, this)
        SourceStatusService.getInstance(project).onReadyChange {
            if (it.isReady) {
                val vertx = UserData.vertx(project)
                vertx.safeLaunch {
                    val service = SourceStatusService.getCurrentService(project)!!
                    ApplicationManager.getApplication().invokeLater {
                        showServiceWindow(service)
                    }
                }
            } else {
                ApplicationManager.getApplication().invokeLater {
                    hideWindows()
                }
            }
        }
        contentManager.addContentManagerListener(this)

        project.messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                if (toolWindow.isVisible) {
                    (contentManager.contents.firstOrNull()?.disposer as? ResumableView)?.onFocused()
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
                SetRefreshIntervalAction(this),
                Separator.getInstance(),
//                ChangeChartAction(),
                ChangeTimeAction(this)
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

    private fun showServiceWindow(service: Service) {
        val overviewWindow = LiveActivityWindow(
            project, service.id, service.name, "Service", listOf(
                MetricType.Service_RespTime_AVG,
                MetricType.Service_SLA,
                MetricType.Service_CPM
            ), 1000
        )
        val overviewContent = contentFactory.createContent(
            overviewWindow.component,
            "Overview",
            true
        )
        overviewContent.setDisposer(overviewWindow)
        overviewContent.isCloseable = false
        contentManager.addContent(overviewContent)

        val endpointsWindow = LiveEndpointsWindow(project, service)
        val endpointsContent = contentFactory.createContent(
            endpointsWindow.component,
            "Endpoints",
            true
        )
        endpointsContent.setDisposer(endpointsWindow)
        endpointsContent.isCloseable = false
        contentManager.addContent(endpointsContent)
    }

    private fun hideWindows() {
        contentManager.contents.forEach { content ->
            contentManager.removeContent(content, true)
        }
    }

    override fun getHistoricalMinutes(): Int? {
        return (currentView as? LiveActivityWindow)?.getHistoricalMinutes()
    }

    override fun setHistoricalMinutes(historicalMinutes: Int) {
        contentManager.contents.mapNotNull { it.disposer as? LiveActivityWindow }.forEach {
            it.setHistoricalMinutes(historicalMinutes)
        }
    }

    override fun showOverviewActivity() = ApplicationManager.getApplication().invokeLater {
        contentManager.setSelectedContent(contentManager.findContent("Overview"))
        toolWindow.show()
    }

    override fun showEndpointActivity(endpoint: ServiceEndpoint) = ApplicationManager.getApplication().invokeLater {
        val existingContent = contentManager.findContent(endpoint.name)
        if (existingContent != null) {
            contentManager.setSelectedContent(existingContent)
            toolWindow.show()
            return@invokeLater
        }

        val activityWindow = LiveActivityWindow(
            project, endpoint.id, endpoint.name, "Endpoint", listOf(
                MetricType.Endpoint_RespTime_AVG.asRealtime(),
                MetricType.Endpoint_SLA.asRealtime(),
                MetricType.Endpoint_CPM.asRealtime()
            ), -1
        )
        activityWindow.resume()

        val content = contentFactory.createContent(
            activityWindow.component,
            endpoint.name,
            false
        )
        content.setDisposer(activityWindow)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        toolWindow.show()
    }

    override fun showChart(
        entityId: String,
        name: String,
        scope: String,
        metricTypes: List<MetricType>
    ) = ApplicationManager.getApplication().invokeLater {
        val existingContent = contentManager.findContent(name)
        if (existingContent != null) {
            contentManager.setSelectedContent(existingContent)
            toolWindow.show()
            return@invokeLater
        }

        val activityWindow = LiveActivityWindow(project, entityId, name, scope, metricTypes, -1)
        activityWindow.resume()

        val content = contentFactory.createContent(
            activityWindow.component,
            name,
            false
        )
        content.setDisposer(activityWindow)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        toolWindow.show()
    }

    override fun dispose() = Unit
}
