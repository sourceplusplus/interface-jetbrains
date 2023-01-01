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
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.plugin.LiveViewTraceService
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.service.view.window.LiveViewTraceWindow
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatusListener
import spp.protocol.platform.general.Service

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewTraceServiceImpl(private val project: Project) : LiveViewTraceService, Disposable {

    companion object {
        fun init(project: Project) {
            project.getService(LiveViewTraceServiceImpl::class.java)
        }
    }

    private var toolWindow: ToolWindow = ToolWindowManager.getInstance(project)
        .registerToolWindow(RegisterToolWindowTask.closable("Live Traces", PluginIcons.diagramSubtask))
    private var contentManager: ContentManager = toolWindow.contentManager

    init {
        project.putUserData(LiveViewTraceService.KEY, this)
        project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
            if (it == SourceStatus.Ready) {
                val vertx = UserData.vertx(project)
                vertx.safeLaunch {
                    val service = ServiceBridge.getCurrentService(vertx)!!
                    showWindow(service)
                }
            } else {
//                ApplicationManager.getApplication().invokeLater {
//                    hideLiveViewChartsWindow()
//                }
            }
        })

        Disposer.register(this, contentManager)
    }

    private fun showWindow(service: Service) = ApplicationManager.getApplication().invokeLater {
        val chartsWindow = LiveViewTraceWindow(project, "todo")
        val overviewContent = ContentFactory.getInstance().createContent(
            chartsWindow.layoutComponent,
            "Service: ${service.name}",
            true
        )
        overviewContent.setDisposer(chartsWindow)
        overviewContent.isCloseable = false
        contentManager.addContent(overviewContent)
    }

    override fun showEndpointTraces(endpointName: String) = ApplicationManager.getApplication().invokeLater {
        val existingContent = contentManager.findContent(endpointName)
        if (existingContent != null) {
            contentManager.setSelectedContent(existingContent)
            toolWindow.show()
            return@invokeLater
        }

        val chartsWindow = LiveViewTraceWindow(project, endpointName)
        val content = ContentFactory.getInstance().createContent(
            chartsWindow.layoutComponent,
            endpointName,
            false
        )
        content.setDisposer(chartsWindow)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        toolWindow.show()
    }

    override fun dispose() = Unit
}
