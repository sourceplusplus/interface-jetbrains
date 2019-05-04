package com.sourceplusplus.plugin.intellij.settings

import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import com.sourceplusplus.plugin.intellij.settings.application.ApplicationSettingsDialogWrapper
import com.sourceplusplus.plugin.intellij.settings.application.EditApplicationSettingsDialogWrapper
import com.sourceplusplus.plugin.intellij.settings.connect.ConnectDialogWrapper
import org.apache.commons.lang.StringUtils
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class PluginSettingsDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(PluginSettingsDialog.class)
    private JPanel contentPane
    private JLabel connectionStatusLabel
    private JButton connectButton
    private JLabel applicationDetailsLabel
    private JButton editApplicationButton
    private JCheckBox agentPatcherEnabledCheckBox
    private ActionListener connectActionListener

    PluginSettingsDialog() {
        setContentPane(contentPane)
        setModal(true)
        editApplicationButton.setVisible(false)
        editApplicationButton.addActionListener(new AbstractAction() {
            @Override
            void actionPerformed(ActionEvent actionEvent) {
                EditApplicationSettingsDialogWrapper applicationSettings = new EditApplicationSettingsDialogWrapper(
                        IntelliJStartupActivity.currentProject)
                applicationSettings.createCenterPanel()
                applicationSettings.show()

                if (applicationSettings.getOkayAction() && !StringUtils.isEmpty(applicationSettings.getApplicationName())) {
                    applicationDetailsLabel.setText(String.format(
                            "<html>Application UUID: %s<br>Application name: %s</html>",
                            SourcePluginConfig.current.appUuid, applicationSettings.getApplicationName()))
                }
            }
        })

        if (PluginBootstrap.getSourcePlugin() != null) {
            updateConnectButton(true, PluginBootstrap.getSourcePlugin().getCoreClient())
        } else if (SourcePluginConfig.current.getEnvironment() == null) {
            updateConnectButton(false, null)
        } else {
            SourceCoreClient newCoreClient = new SourceCoreClient(
                    SourcePluginConfig.current.getEnvironment().getSppUrl())
            if (SourcePluginConfig.current.getEnvironment().apiKey != null) {
                newCoreClient.setApiKey(SourcePluginConfig.current.getEnvironment().apiKey)
            }
            newCoreClient.ping({
                if (it.succeeded()) {
                    updateConnectButton(true, newCoreClient)
                } else {
                    updateConnectButton(false, null)
                }
            })
        }
    }

    private void updateConnectButton(boolean isConnected, SourceCoreClient coreClient) {
        if (connectActionListener != null) {
            connectButton.removeActionListener(connectActionListener)
            connectActionListener = null
        }
        if (isConnected) {
            if (PluginBootstrap.getSourcePlugin() == null && SourcePluginConfig.current.appUuid != null) {
                IntelliJStartupActivity.startSourcePlugin(coreClient)
            }

            applicationDetailsLabel.setText("")
            connectionStatusLabel.setText("Status: Connected")
            connectButton.setText("Create/Assign Application")
            connectButton.addActionListener(connectActionListener = new AbstractAction() {
                @Override
                void actionPerformed(ActionEvent actionEvent) {
                    ApplicationSettingsDialogWrapper applicationSettings = new ApplicationSettingsDialogWrapper(
                            IntelliJStartupActivity.currentProject, coreClient)
                    applicationSettings.createCenterPanel()
                    applicationSettings.show()

                    if (applicationSettings.getOkayAction()) {
                        updateApplicationDetails(coreClient)

                        if (PluginBootstrap.getSourcePlugin() == null && SourcePluginConfig.current.appUuid != null) {
                            IntelliJStartupActivity.startSourcePlugin(coreClient)
                        }
                    }
                }
            })
            updateApplicationDetails(coreClient)
        } else {
            applicationDetailsLabel.setText("")
            connectionStatusLabel.setText("Status: Not Connected")
            connectButton.setText("Connect")
            connectButton.addActionListener(connectActionListener = new AbstractAction() {
                @Override
                void actionPerformed(ActionEvent actionEvent) {
                    ConnectDialogWrapper connectDialog = new ConnectDialogWrapper(IntelliJStartupActivity.currentProject)
                    connectDialog.createCenterPanel()
                    connectDialog.show()

                    if (SourcePluginConfig.current.environment) {
                        SourceCoreClient newCoreClient = new SourceCoreClient(
                                SourcePluginConfig.current.getEnvironment().getSppUrl())
                        if (SourcePluginConfig.current.getEnvironment().apiKey != null) {
                            newCoreClient.setApiKey(SourcePluginConfig.current.getEnvironment().apiKey)
                        }
                        newCoreClient.ping({
                            if (it.succeeded()) {
                                updateConnectButton(true, newCoreClient)
                            } else {
                                log.error("Failed to connect to Source++ Core", it.cause())
                            }
                        })
                    }
                }
            })
        }
    }

    private void updateApplicationDetails(SourceCoreClient coreClient) {
        if (SourcePluginConfig.current.appUuid != null) {
            coreClient.getApplication(SourcePluginConfig.current.appUuid, {
                if (it.succeeded()) {
                    if (it.result().isPresent()) {
                        applicationDetailsLabel.setText(String.format(
                                "<html>Application UUID: %s<br>Application name: %s</html>",
                                it.result().get().appUuid(), it.result().get().appName()))
                        editApplicationButton.setVisible(true)
                    }
                } else {
                    log.error("Failed to get application", it.cause())
                }
            })
        }
    }

    void setData(@NotNull SourcePluginConfig config) {
        agentPatcherEnabledCheckBox.setSelected(config.agentPatcherEnabled)
    }

    void getData(@NotNull SourcePluginConfig data) {
        data.agentPatcherEnabled = agentPatcherEnabledCheckBox.isSelected()
    }

    boolean isModified(@NotNull SourcePluginConfig data) {
        return agentPatcherEnabledCheckBox.isSelected() != data.agentPatcherEnabled
    }

    void setDataCustom(@NotNull SourcePluginConfig settings) {
        setData(settings)
    }
}
