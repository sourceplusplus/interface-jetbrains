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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.view.action.ResumeViewAction
import spp.jetbrains.sourcemarker.view.action.StopViewAction
import spp.jetbrains.sourcemarker.view.window.LiveViewTraceWindowImpl
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatusListener
import spp.jetbrains.view.LiveViewTraceManager
import spp.jetbrains.view.ResumableView
import spp.jetbrains.view.window.LiveTraceWindow
import spp.protocol.platform.general.Service
import spp.protocol.view.LiveView

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewTraceManagerImpl(
    private val project: Project
) : LiveViewTraceManager, ContentManagerListener {

    companion object {
        fun init(project: Project) {
            project.getService(LiveViewTraceManagerImpl::class.java)
        }
    }

    private var toolWindow: ToolWindow = ToolWindowManager.getInstance(project)
        .registerToolWindow(RegisterToolWindowTask.closable("Live Traces", PluginIcons.diagramSubtask))
    private var contentManager: ContentManager = toolWindow.contentManager
    override var currentView: ResumableView? = null

    init {
        project.putUserData(LiveViewTraceManager.KEY, this)
        project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
            if (it == SourceStatus.Ready) {
                val vertx = UserData.vertx(project)
                vertx.safeLaunch {
                    val service = ServiceBridge.getCurrentService(vertx)!!
                    showWindow(service)
                }
            } else {
                ApplicationManager.getApplication().invokeLater {
                    hideWindow()
                }
            }
        })
        contentManager.addContentManagerListener(this)

        //pause views when tool window is closed
        project.messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager, changeType: ToolWindowManagerEventType) {
                if (!toolWindow.isVisible) {
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
                StopViewAction(this)
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

    private fun showWindow(service: Service) = ApplicationManager.getApplication().invokeLater {
//        val traceWindow = LiveViewTraceWindow(project)
//        val overviewContent = ContentFactory.getInstance().createContent(
//            traceWindow.layoutComponent,
//            "Service: ${service.name}",
//            true
//        )
//        overviewContent.setDisposer(traceWindow)
//        overviewContent.isCloseable = false
//        contentManager.addContent(overviewContent)
    }

    private fun hideWindow() {
        contentManager.contents.forEach { content ->
            contentManager.removeContent(content, true)
        }
    }

    override fun showEndpointTraces(
        liveView: LiveView,
        endpointName: String,
        consumer: (LiveTraceWindow) -> MessageConsumer<JsonObject>
    ) = ApplicationManager.getApplication().invokeLater {
        val existingContent = contentManager.findContent(endpointName)
        if (existingContent != null) {
            contentManager.setSelectedContent(existingContent)
            toolWindow.show()
            return@invokeLater
        }

        val traceWindow = LiveViewTraceWindowImpl(project, liveView, consumer)
        val content = ContentFactory.getInstance().createContent(
            traceWindow.layoutComponent,
            endpointName,
            false
        )
        content.setDisposer(traceWindow)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        toolWindow.show()
    }

    override fun dispose() = Unit
}
