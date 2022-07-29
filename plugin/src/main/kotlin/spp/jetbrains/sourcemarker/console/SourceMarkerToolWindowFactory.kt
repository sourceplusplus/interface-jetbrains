/*
 * Source++, the open-source live coding platform.
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
package spp.jetbrains.sourcemarker.console

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import org.slf4j.LoggerFactory
import spp.jetbrains.sourcemarker.SourceMarkerPlugin

/**
 * Displays logs from the SourceMarker plugin to a console window.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerToolWindowFactory : ToolWindowFactory {

    companion object {
        private val log = LoggerFactory.getLogger(SourceMarkerToolWindowFactory::class.java)
    }

    override fun isApplicable(project: Project): Boolean {
        return SourceMarkerPlugin.getConfig(project).pluginConsoleEnabled
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val consoleService = ServiceManager.getService(project, SourceMarkerConsoleService::class.java)
        val consoleView = consoleService.getConsoleView()
        val content = toolWindow.contentManager.factory.createContent(consoleView.component, "", true)
        toolWindow.contentManager.addContent(content)
        toolWindow.setAnchor(ToolWindowAnchor.BOTTOM, null)
        toolWindow.isAvailable = true

        SourceMarkerAppender.consoleView = consoleView
        log.info("Internal console enabled")
    }
}
