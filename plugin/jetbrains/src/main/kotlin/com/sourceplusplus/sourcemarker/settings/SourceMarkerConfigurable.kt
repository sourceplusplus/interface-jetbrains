package com.sourceplusplus.sourcemarker.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import io.vertx.core.json.Json
import javax.swing.JComponent

/**
 * Used to view and edit plugin configuration.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerConfigurable : Configurable {

    private var form: PluginConfigurationDialog? = null
    override fun getDisplayName(): String = "SourceMarker"
    override fun isModified(): Boolean = form!!.isModified

    override fun apply() {
        val updatedConfig = form!!.pluginConfig
        PropertiesComponent.getInstance().setValue(
            "sourcemarker_plugin_config",
            Json.encode(updatedConfig)
        )
        form!!.applySourceMarkerConfig(updatedConfig)
    }

    override fun createComponent(): JComponent {
        if (form == null) {
            val config = if (
                PropertiesComponent.getInstance().isValueSet("sourcemarker_plugin_config")
            ) {
                Json.decodeValue(
                    PropertiesComponent.getInstance().getValue("sourcemarker_plugin_config"),
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