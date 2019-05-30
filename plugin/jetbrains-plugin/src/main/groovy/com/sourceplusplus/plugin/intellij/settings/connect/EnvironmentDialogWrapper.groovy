package com.sourceplusplus.plugin.intellij.settings.connect

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.sourceplusplus.api.model.config.SourcePluginConfig
import org.jetbrains.annotations.Nullable

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class EnvironmentDialogWrapper extends DialogWrapper {

    private final EnvironmentDialog environmentDialog = new EnvironmentDialog()
    private final Project project

    EnvironmentDialogWrapper(@Nullable Project project) {
        super(project)
        this.project = project
        init()
        setTitle("Manage Environments")
        setResizable(false)
        environmentDialog.setData(SourcePluginConfig.current)
    }

    @Nullable
    @Override
    JComponent createCenterPanel() {
        return environmentDialog.getContentPane()
    }

    @Override
    protected void doOKAction() {
        SourcePluginConfig.current.applyConfig(getConfig())
        project.save()
        super.doOKAction()
    }

    private SourcePluginConfig getConfig() {
        SourcePluginConfig config = SourcePluginConfig.current.clone()
        environmentDialog.getData(config)
        return config
    }
}
