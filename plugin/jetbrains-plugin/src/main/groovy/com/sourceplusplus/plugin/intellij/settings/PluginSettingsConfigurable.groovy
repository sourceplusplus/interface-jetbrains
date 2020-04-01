package com.sourceplusplus.plugin.intellij.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import io.vertx.core.json.Json
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import plus.sourceplus.marker.plugin.SourceMarkerPlugin

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class PluginSettingsConfigurable implements Configurable {

    private PluginSettingsDialog form

    @Nls
    @NotNull
    @Override
    String getDisplayName() {
        return "Source++"
    }

    @NonNls
    @Nullable
    @Override
    String getHelpTopic() {
        return null
    }

    @NotNull
    @Override
    JComponent createComponent() {
        if (form == null) {
            form = new PluginSettingsDialog()
        }
        return form.contentPane as JComponent
    }

    @Override
    boolean isModified() {
        SourcePluginConfig pluginConfig = SourcePluginConfig.current.clone()
        return form.isModified(pluginConfig)
    }

    @Override
    void apply() throws ConfigurationException {
        SourcePluginConfig pluginConfig = SourcePluginConfig.current.clone()
        if (form != null) {
            form.getData(pluginConfig)

            SourcePluginConfig.current.applyConfig(pluginConfig)
            PropertiesComponent.getInstance().setValue(
                    "spp_plugin_config", Json.encode(SourcePluginConfig.current))

            SourceMarkerPlugin.INSTANCE.refreshActiveSourceFileMarkers()
        }
    }

    @Override
    void reset() {
        if (form != null) {
            form.setDataCustom(SourcePluginConfig.current)
        }
    }

    @Override
    void disposeUIResources() {
        form = null
    }
}