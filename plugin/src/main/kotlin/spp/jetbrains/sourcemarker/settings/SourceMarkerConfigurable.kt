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
package spp.jetbrains.sourcemarker.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.ProjectManager
import io.vertx.core.json.Json
import kotlinx.coroutines.runBlocking
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import javax.swing.JComponent

/**
 * Used to view and edit plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfigurable : Configurable {

    private var form: PluginConfigurationPanel? = null
    override fun getDisplayName(): String = message("plugin_name")

    override fun isModified(): Boolean {
        val projectSettings = PropertiesComponent.getInstance(ProjectManager.getInstance().openProjects[0])
        return !projectSettings.isValueSet("sourcemarker_plugin_config") || form!!.isModified
    }

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
            val config = SourceMarkerPlugin.getConfig(ProjectManager.getInstance().openProjects[0])
            form = PluginConfigurationPanel(config)
            form!!.applySourceMarkerConfig(config)
        }
        return form!!.contentPane as JComponent
    }

    override fun disposeUIResources() {
        form = null
    }
}
