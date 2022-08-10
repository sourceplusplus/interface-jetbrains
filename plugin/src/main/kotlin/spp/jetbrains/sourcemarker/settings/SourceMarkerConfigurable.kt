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
package spp.jetbrains.sourcemarker.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.protocol.SourceServices
import javax.swing.JComponent

/**
 * Used to view and edit plugin configuration.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfigurable(val project: Project) : Configurable {

    private var form: PluginConfigurationPanel? = null

    override fun getDisplayName(): String = message("plugin_name")
    override fun isModified(): Boolean = form!!.isModified

    override fun apply() {
        val updatedConfig = form!!.pluginConfig
        val projectSettings = PropertiesComponent.getInstance(project)
        projectSettings.setValue("sourcemarker_plugin_config", Json.encode(updatedConfig))
        form!!.applySourceMarkerConfig(updatedConfig)

        DumbService.getInstance(project).smartInvokeLater {
            runBlocking {
                SourceMarkerPlugin.getInstance(project).init()
            }
        }
    }

    override fun createComponent(): JComponent {
        if (form == null) {
            val config = SourceMarkerPlugin.getInstance(project).getConfig()
            val availServices = runBlocking {
                SourceServices.Instance.liveService?.getServices()?.let { it.await() } ?: emptyList()
            }
            form = PluginConfigurationPanel(config, availServices)
            form!!.applySourceMarkerConfig(config)
        }
        return form!!.contentPane as JComponent
    }

    override fun disposeUIResources() {
        form = null
    }
}
