package com.sourceplusplus.sourcemarker.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.ProjectManager
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import io.vertx.core.json.Json
import kotlinx.coroutines.runBlocking
import javax.swing.JComponent

/**
 * Used to view and edit plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfigurable : Configurable {

    private var form: PluginConfigurationDialog? = null
    override fun getDisplayName(): String = "SourceMarker"
    override fun isModified(): Boolean = form!!.isModified

    override fun apply() {
        val updatedConfig = form!!.pluginConfig
        val projectSettings = PropertiesComponent.getInstance(ProjectManager.getInstance().openProjects[0])
        projectSettings.setValue("sourcemarker_plugin_config", Json.encode(updatedConfig))
        form!!.applySourceMarkerConfig(updatedConfig)

        val activeProject = ProjectManager.getInstance().openProjects[0]
        DumbService.getInstance(activeProject).smartInvokeLater {
            runBlocking {
                SourceMarkerPlugin.init(activeProject)
            }
        }
    }

    override fun createComponent(): JComponent {
        if (form == null) {
            val projectSettings = PropertiesComponent.getInstance(ProjectManager.getInstance().openProjects[0])
            val config = if (projectSettings.isValueSet("sourcemarker_plugin_config")) {
                Json.decodeValue(
                    projectSettings.getValue("sourcemarker_plugin_config"),
                    SourceMarkerConfig::class.java
                )
            } else {
                SourceMarkerConfig()
            }
            form = PluginConfigurationDialog()
            form!!.applySourceMarkerConfig(config)
        }
        return form!!.contentPane as JComponent
    }

    override fun disposeUIResources() {
        form = null
    }
}
