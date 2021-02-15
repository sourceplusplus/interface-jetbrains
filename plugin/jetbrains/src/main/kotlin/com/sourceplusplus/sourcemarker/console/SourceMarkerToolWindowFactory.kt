package com.sourceplusplus.sourcemarker.console

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.sourceplusplus.sourcemarker.settings.SourceMarkerConfig
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
        log.info("Internal console enabled")
    }
}
