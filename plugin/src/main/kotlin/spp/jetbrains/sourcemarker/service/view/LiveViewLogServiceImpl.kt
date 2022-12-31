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

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
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
import spp.jetbrains.plugin.LiveViewLogService
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.service.view.action.StopLogsAction
import spp.jetbrains.sourcemarker.service.view.action.ResumeLogsAction
import spp.jetbrains.sourcemarker.service.view.window.LiveLogWindow
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatusListener
import spp.jetbrains.status.SourceStatusService
import spp.protocol.platform.general.Service

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewLogServiceImpl(
    private val project: Project
) : LiveViewLogService, ContentManagerListener, Disposable {

    companion object {
        fun init(project: Project) {
            project.getService(LiveViewLogServiceImpl::class.java)
        }
    }

    private val log = logger<LiveViewLogServiceImpl>()
    private var toolWindow = ToolWindowManager.getInstance(project)
        .registerToolWindow(RegisterToolWindowTask.closable("Live Logs", PluginIcons.messageLines))
    private var contentManager = toolWindow.contentManager
    private lateinit var serviceLogsWindow: LiveLogWindow

    init {
        project.putUserData(LiveViewLogService.KEY, this)
        project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
            if (it != SourceStatus.Ready) {
                ApplicationManager.getApplication().invokeLater {
                    hideWindow()
                }
            }
        })
        contentManager.addContentManagerListener(this)

        project.messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager, changeType: ToolWindowManagerEventType) {
                if (toolWindow.isVisible && SourceStatusService.getInstance(project).isReady()) {
                    if (!::serviceLogsWindow.isInitialized) {
                        val vertx = UserData.vertx(project)
                        vertx.safeLaunch {
                            val service = ServiceBridge.getCurrentService(vertx)!!
                            showWindow(service)
                        }
                    }
                } else if (::serviceLogsWindow.isInitialized && !toolWindow.isVisible) {
                    serviceLogsWindow.pause()
                }
            }
        })

        Disposer.register(this, contentManager)
    }

    override fun selectionChanged(event: ContentManagerEvent) {
        println(event)
    }

    private fun showWindow(service: Service) = ApplicationManager.getApplication().invokeLater {
        serviceLogsWindow = LiveLogWindow(project, service)
        toolWindow.setTitleActions(
            listOf(
                ResumeLogsAction(serviceLogsWindow),
                StopLogsAction(serviceLogsWindow)
            )
        )

        val content = ContentFactory.getInstance().createContent(
            serviceLogsWindow.component,
            "Service: ${service.name}",
            true
        )
        content.setDisposer(serviceLogsWindow)
        content.isCloseable = false
        contentManager.addContent(content)

        serviceLogsWindow.resume()
    }

    private fun hideWindow() {
        toolWindow.setTitleActions(emptyList())
        contentManager.contents.forEach { content ->
            contentManager.removeContent(content, true)
        }
    }

    override fun showInConsole(
        message: String,
        consoleTitle: String,
        project: Project,
        contentType: ConsoleViewContentType,
        scrollTo: Int
    ): ConsoleView {
        val liveLogWindow = LiveLogWindow(project)
        ApplicationManager.getApplication().invokeLater {
            val content = ContentFactory.getInstance().createContent(
                liveLogWindow.component,
                "Endpoint Logs",
                false
            )
            content.setDisposer(liveLogWindow)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)

            toolWindow.show()
        }

        return liveLogWindow.showInConsole(message, project, contentType, scrollTo)
    }

    override fun dispose() = Unit
}
