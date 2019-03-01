package com.sourceplusplus.plugin.intellij.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.sourceplusplus.api.model.config.SourcePluginConfig
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * todo: description
 *
 * @version 0.1.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@State(name = "Source++", storages = [@Storage(file = '$APP_CONFIG$/Source++.xml')])
class PluginSettingsComponent implements ApplicationComponent, PersistentStateComponent<SourcePluginConfig> {

    @NotNull
    @Override
    String getComponentName() {
        return "Source++"
    }

    @NotNull
    @Override
    SourcePluginConfig getState() {
        return SourcePluginConfig.current
    }

    @Override
    void loadState(@Nullable SourcePluginConfig state) {
        if (state != null) {
            SourcePluginConfig.current.applyConfig(state)
        }
    }

    protected static PluginSettingsComponent getInstance() {
        return ApplicationManager.getApplication().getComponent(PluginSettingsComponent.class)
    }
}
