package com.sourceplusplus.plugin.intellij.settings.connect

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.sourceplusplus.api.model.config.SourcePluginConfig
import org.jetbrains.annotations.Nullable

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.1.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ConnectDialogWrapper extends DialogWrapper {

    private final ConnectDialog connectDialog = new ConnectDialog()
    private final Project project
    private boolean startPlugin

    ConnectDialogWrapper(@Nullable Project project) {
        super(project)
        this.project = project
        init()
        setTitle("Connect Source++")
        setResizable(false)
    }

    boolean getStartPlugin() {
        return startPlugin
    }

    @Nullable
    @Override
    JComponent createCenterPanel() {
        return connectDialog.getContentPane()
    }

    @Override
    protected void doOKAction() {
        def fullHost = connectDialog.host
        if (fullHost.startsWith("https://")) {
            SourcePluginConfig.current.apiSslEnabled = true
            def hostParts = fullHost.substring(8).split(":")
            SourcePluginConfig.current.apiHost = hostParts[0]
            if (hostParts.length > 1) {
                SourcePluginConfig.current.apiPort = Integer.parseInt(hostParts[1])
            } else {
                SourcePluginConfig.current.apiPort = 443
            }
        } else {
            SourcePluginConfig.current.apiSslEnabled = false
            def hostParts = fullHost.substring(7).split(":")
            SourcePluginConfig.current.apiHost = hostParts[0]
            if (hostParts.length > 1) {
                SourcePluginConfig.current.apiPort = Integer.parseInt(hostParts[1])
            } else {
                SourcePluginConfig.current.apiPort = 80
            }
        }
        SourcePluginConfig.current.apiKey = connectDialog.apiToken

        startPlugin = true
        super.doOKAction()
    }

    @Override
    void doCancelAction() {
        super.doCancelAction()
    }
}
