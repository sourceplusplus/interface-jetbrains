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
class ConnectDialogWrapper extends DialogWrapper {

    private final EnvironmentDialog connectDialog = new EnvironmentDialog()
    private final Project project

    ConnectDialogWrapper(@Nullable Project project) {
        super(project)
        this.project = project
        init()
        setTitle("Connect Source++")
        setResizable(false)
        connectDialog.setData(SourcePluginConfig.current)
    }

    @Nullable
    @Override
    JComponent createCenterPanel() {
        return connectDialog.getContentPane()
    }

    @Override
    protected void doOKAction() {
        project.save()
        super.doOKAction()
    }
}
