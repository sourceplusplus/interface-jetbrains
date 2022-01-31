/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.console

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import org.slf4j.LoggerFactory

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
        return if (PropertiesComponent.getInstance(project).isValueSet("sourcemarker_plugin_config")) {
            try {
                val config = Json.decodeValue(
                    PropertiesComponent.getInstance(project).getValue("sourcemarker_plugin_config"),
                    SourceMarkerConfig::class.java
                )
                config.pluginConsoleEnabled
            } catch (ignore: DecodeException) {
                false

            }
        } else {
            false
        }
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
