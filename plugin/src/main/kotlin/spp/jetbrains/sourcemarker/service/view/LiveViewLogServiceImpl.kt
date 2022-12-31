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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.plugin.LiveViewLogService
import spp.jetbrains.sourcemarker.service.view.window.LiveLogWindow
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatusListener

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewLogServiceImpl(private val project: Project) : LiveViewLogService, Disposable {

    companion object {
        fun init(project: Project) {
            project.getService(LiveViewLogServiceImpl::class.java)
        }
    }

    private var toolWindow: ToolWindow = ToolWindowManager.getInstance(project)
        .registerToolWindow(RegisterToolWindowTask.closable("Live Logs", PluginIcons.messageLines))
    private var contentManager: ContentManager = toolWindow.contentManager

    init {
        project.putUserData(LiveViewLogService.KEY, this)
        project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
            if (it == SourceStatus.Ready) {
//                val vertx = UserData.vertx(project)
//                vertx.safeLaunch {
//                    val service = ServiceBridge.getCurrentService(vertx)!!
//                    showLiveViewTraceWindow(service)
//                }
            } else {
//                ApplicationManager.getApplication().invokeLater {
//                    hideLiveViewChartsWindow()
//                }
            }
        })

        Disposer.register(this, contentManager)

        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun contentAdded(contentManagerEvent: ContentManagerEvent) = Unit
            override fun contentRemoved(event: ContentManagerEvent) {
                if (toolWindow.contentManager.contentCount == 0) {
                    toolWindow.setAvailable(false, null)
                }
            }
        })
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
                liveLogWindow.layoutComponent,
                "Service Logs",
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
