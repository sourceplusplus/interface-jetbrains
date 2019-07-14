package com.sourceplusplus.plugin.intellij.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.2.2
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
        return form.isModified(PluginSettingsComponent.getInstance().getState())
    }

    @Override
    void apply() throws ConfigurationException {
        SourcePluginConfig settings = PluginSettingsComponent.getInstance().getState()
        if (form != null) {
            form.getData(settings)

            SourcePluginConfig.current.applyConfig(settings)
            PluginBootstrap.sourcePlugin.refreshActiveSourceFileMarkers()
        }
    }

    @Override
    void reset() {
        SourcePluginConfig settings = PluginSettingsComponent.getInstance().getState()
        if (form != null) {
            form.setDataCustom(settings)
        }
    }

    @Override
    void disposeUIResources() {
        form = null
    }
}