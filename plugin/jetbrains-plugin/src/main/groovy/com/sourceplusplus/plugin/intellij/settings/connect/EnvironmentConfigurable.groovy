package com.sourceplusplus.plugin.intellij.settings.connect

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.2.3
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class EnvironmentConfigurable implements Configurable {

    private EnvironmentDialog form

    @Nls
    @NotNull
    @Override
    String getDisplayName() {
        return "Manage Environments"
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
            form = new EnvironmentDialog()
        }
        return form.getContentPane()
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

            PluginBootstrap.sourcePlugin.clearActiveSourceFileMarkers()
            def coreClient = new SourceCoreClient(pluginConfig.activeEnvironment.sppUrl)
            if (pluginConfig.activeEnvironment.apiKey) {
                coreClient.apiKey = pluginConfig.activeEnvironment.apiKey
            }
            coreClient.ping({
                if (it.succeeded()) {
                    PluginBootstrap.sourcePlugin.updateEnvironment(coreClient)

                    if (pluginConfig.activeEnvironment?.appUuid) {
                        coreClient.getApplication(pluginConfig.activeEnvironment.appUuid, {
                            if (it.succeeded() && it.result().isPresent()) {
                                FileEditorManager manager = FileEditorManager.getInstance(IntelliJStartupActivity.currentProject)
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    void run() {
                                        manager.getSelectedFiles().each {
                                            def psiFile = PsiManager.getInstance(IntelliJStartupActivity.currentProject).findFile(it)
                                            psiFile.virtualFile.putUserData(IntelliJSourceFileMarker.KEY, null)
                                            IntelliJStartupActivity.coordinateSourceFileOpened(
                                                    PluginBootstrap.sourcePlugin, (PsiClassOwner) psiFile)
                                        }
                                        PluginBootstrap.sourcePlugin.refreshActiveSourceFileMarkers()
                                    }
                                })
                            } else if (it.succeeded()) {
                                pluginConfig.activeEnvironment.appUuid = null
                            } else {
                                log.error("Failed to get application", it.cause())
                            }
                        })
                    }
                } else {
                    pluginConfig.activeEnvironment.appUuid = null
                }
            })
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