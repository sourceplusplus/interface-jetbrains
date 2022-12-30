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
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.sourcemarker.service.view.window.LiveViewTraceWindow

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewTraceService(private val project: Project) : Disposable {

    companion object {
        fun getInstance(project: Project): LiveViewTraceService {
            return project.getService(LiveViewTraceService::class.java)
        }
    }

    private var _toolWindow: ToolWindow? = null
    private var contentManager: ContentManager? = null

    init {
        _toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(
            RegisterToolWindowTask.closable("Live Traces", PluginIcons.diagramSubtask)
        )
        contentManager = _toolWindow!!.contentManager

        _toolWindow!!.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun contentAdded(contentManagerEvent: ContentManagerEvent) = Unit
            override fun contentRemoved(event: ContentManagerEvent) {
                if (_toolWindow!!.contentManager.contentCount == 0) {
                    _toolWindow!!.setAvailable(false, null)
                }
            }
        })
    }

    fun showLiveViewTraceWindow(endpointName: String) {
        val chartsWindow = LiveViewTraceWindow(project, endpointName)
        val content = ContentFactory.getInstance().createContent(
            chartsWindow.layoutComponent,
            "/" + endpointName.substringAfterLast("/"),
            false
        )
        content.setDisposer(chartsWindow)
        contentManager!!.addContent(content)
    }

    override fun dispose() = Unit
}
