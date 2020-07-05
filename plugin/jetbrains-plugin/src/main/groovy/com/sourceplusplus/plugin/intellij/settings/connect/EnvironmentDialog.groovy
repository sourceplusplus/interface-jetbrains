package com.sourceplusplus.plugin.intellij.settings.connect

import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.config.SourceEnvironmentConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.integration.ConnectionType
import com.sourceplusplus.api.model.integration.IntegrationConnection
import com.sourceplusplus.api.model.integration.IntegrationInfo
import com.sourceplusplus.api.model.integration.config.ApacheSkyWalkingIntegrationConfig
import groovy.util.logging.Slf4j
import org.jetbrains.annotations.NotNull

import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Used to create, remove, and configure core environments.
 *
 * @version 0.3.1
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class EnvironmentDialog extends JDialog {

    private JPanel contentPane
    private JList<SourceEnvironmentConfig> environmentList
    private JButton createButton
    private JButton deleteButton
    private JButton addIntegrationButton
    private JButton saveButton
    private JButton testConnectionButton
    private JSpinner portSpinner
    private JTextField nameTextField
    private JTextField hostTextField
    private JTextField apiTokenTextField
    private JButton activateButton
    private JCheckBox sslEnabledCheckbox
    private SourceEnvironmentConfig activeEnvironment

    EnvironmentDialog() {
        setContentPane(contentPane)
        setModal(true)
        portSpinner.model = new SpinnerNumberModel(8080, 0, 65535, 1)
        portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"))
        environmentList.setModel(new DefaultListModel<SourceEnvironmentConfig>())

        createButton.addActionListener({
            environmentList.clearSelection()
            clearConnectionForm(true)
            nameTextField.requestFocus()
        })
        addIntegrationButton.addActionListener({
            def integrations = ["Apache SkyWalking"] as String[]
            String integration = (String) JOptionPane.showInputDialog(this, "Integration to add to Source++ Core",
                    "Add Integration", JOptionPane.QUESTION_MESSAGE, null, integrations, integrations[0])
            if (integration == "Apache SkyWalking") {
                def grpcAddress = JOptionPane.showInputDialog(this, "Server GRPC address (e.g. localhost:11800)",
                        "Apache SkyWalking OAP Server", JOptionPane.QUESTION_MESSAGE)
                if (grpcAddress) {
                    def restAddress = JOptionPane.showInputDialog(this, "Server REST address (e.g. localhost:12800)",
                            "Apache SkyWalking OAP Server", JOptionPane.QUESTION_MESSAGE)
                    if (restAddress) {
                        def restConnection = IntegrationConnection.builder()
                                .host(restAddress.split(":")[0]).port(restAddress.split(":")[1] as int)
                                .build()
                        def grpcConnection = IntegrationConnection.builder()
                                .host(grpcAddress.split(":")[0]).port(grpcAddress.split(":")[1] as int)
                                .build()
                        def integrationInfo = IntegrationInfo.builder()
                                .id("apache_skywalking")
                                .enabled(true)
                                .putConnections(ConnectionType.REST, restConnection)
                                .putConnections(ConnectionType.gRPC, grpcConnection)
                                .config(ApacheSkyWalkingIntegrationConfig.builder().build()).build()

                        def host = hostTextField.getText()
                        def coreClient = new SourceCoreClient(host, portSpinner.value as int, sslEnabledCheckbox.isSelected())
                        if (!apiTokenTextField.getText().isAllWhitespace()) {
                            coreClient.apiKey = apiTokenTextField.getText().trim()
                        }
                        coreClient.updateIntegrationInfo(integrationInfo, {
                            if (it.succeeded()) {
                                JOptionPane.showMessageDialog(this, "Integration has been successfully added.",
                                        "Successfully Updated", JOptionPane.INFORMATION_MESSAGE)
                            } else {
                                log.error("Failed to update integration", it.cause())
                                JOptionPane.showMessageDialog(this, it.cause().message,
                                        "Update Failed", JOptionPane.WARNING_MESSAGE)
                            }
                        })
                    }
                }
            }
        })

        environmentList.addListSelectionListener({
            if (environmentList.selectedValue != null) {
                clearConnectionForm(true)
                addIntegrationButton.setEnabled(true)

                def env = environmentList.selectedValue as SourceEnvironmentConfig
                nameTextField.text = env.environmentName
                hostTextField.text = env.apiHost
                portSpinner.value = env.apiPort
                apiTokenTextField.text = env.apiKey
                sslEnabledCheckbox.setSelected(env.apiSslEnabled)

                def existingEnvironment = SourcePluginConfig.current.getEnvironment(env.environmentName)
                if (existingEnvironment != null && existingEnvironment.embedded) {
                    nameTextField.setEnabled(false)
                    hostTextField.setEnabled(false)
                    portSpinner.setEnabled(false)
                    apiTokenTextField.setEnabled(false)
                    sslEnabledCheckbox.setEnabled(false)
                    deleteButton.setEnabled(false)
                } else {
                    deleteButton.setEnabled(true)
                    nameTextField.requestFocus()
                }

                if (activeEnvironment == null && environmentList.model.size == 1) {
                    //only environment automatically becomes active environment
                    activateButton.setEnabled(false)
                } else {
                    if (env.embedded && !SourcePluginConfig.current.embeddedCoreServer) {
                        activateButton.setEnabled(false)
                    } else {
                        activateButton.setEnabled(activeEnvironment != env)
                    }
                }
            } else {
                deleteButton.setEnabled(false)
                activateButton.setEnabled(false)
                addIntegrationButton.setEnabled(false)
            }
        })
        deleteButton.addActionListener({
            if (environmentList.selectedValue != null) {
                DefaultListModel<SourceEnvironmentConfig> model = environmentList.getModel()
                int selectedIndex = environmentList.getSelectedIndex()
                if (selectedIndex != -1) {
                    model.remove(selectedIndex)
                    clearConnectionForm(false)
                }
            }
        })
        activateButton.addActionListener({
            if (environmentList.selectedValue != null) {
                activeEnvironment = environmentList.getModel().getElementAt(environmentList.getSelectedIndex())
                activateButton.setEnabled(false)
            }
        })
        saveButton.addActionListener({
            def setAsActive = false
            def addIndex = environmentList.model.size
            if (environmentList.selectedValue != null) {
                //update environment
                def env = environmentList.getModel().getElementAt(environmentList.getSelectedIndex())
                setAsActive = activeEnvironment == env
                addIndex = environmentList.getSelectedIndex()
                (environmentList.model as DefaultListModel<SourceEnvironmentConfig>).remove(environmentList.getSelectedIndex())
            }

            def env = new SourceEnvironmentConfig()
            env.environmentName = nameTextField.text
            env.apiHost = hostTextField.text
            env.apiPort = portSpinner.value as int
            env.apiSslEnabled = sslEnabledCheckbox.isSelected()
            if (!apiTokenTextField.getText().isAllWhitespace()) {
                env.apiKey = apiTokenTextField.text
            }
            clearConnectionForm(false)
            (environmentList.model as DefaultListModel<SourceEnvironmentConfig>).add(addIndex, env)

            if (setAsActive) {
                activeEnvironment = env
            }
        })
        testConnectionButton.addActionListener({
            def host = hostTextField.getText()
            def coreClient = new SourceCoreClient(host, portSpinner.value as int, sslEnabledCheckbox.isSelected())
            if (!apiTokenTextField.getText().isAllWhitespace()) {
                coreClient.apiKey = apiTokenTextField.getText().trim()
            }

            coreClient.ping({
                if (it.failed()) {
                    def connectDialog = new ConnectionInfoDialogWrapper(it.cause())
                    connectDialog.createCenterPanel()
                    connectDialog.show()
                } else {
                    coreClient.info({
                        if (it.failed()) {
                            def connectDialog = new ConnectionInfoDialogWrapper(it.cause())
                            connectDialog.createCenterPanel()
                            connectDialog.show()
                        } else {
                            def connectDialog = new ConnectionInfoDialogWrapper(it.result())
                            connectDialog.createCenterPanel()
                            connectDialog.show()
                        }
                    })
                }
            })
        })
        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            void insertUpdate(DocumentEvent documentEvent) {
                updateButtons()
            }

            @Override
            void removeUpdate(DocumentEvent documentEvent) {
            }

            @Override
            void changedUpdate(DocumentEvent documentEvent) {
            }
        })
        hostTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            void insertUpdate(DocumentEvent documentEvent) {
                updateButtons()
            }

            @Override
            void removeUpdate(DocumentEvent documentEvent) {
            }

            @Override
            void changedUpdate(DocumentEvent documentEvent) {
            }
        })
    }

    private void clearConnectionForm(boolean enabled) {
        nameTextField.text = ""
        hostTextField.text = ""
        portSpinner.value = 8080
        apiTokenTextField.text = ""
        sslEnabledCheckbox.setSelected(false)

        nameTextField.setEnabled(enabled)
        hostTextField.setEnabled(enabled)
        portSpinner.setEnabled(enabled)
        apiTokenTextField.setEnabled(enabled)
        sslEnabledCheckbox.setEnabled(enabled)
        testConnectionButton.setEnabled(enabled)
        saveButton.setEnabled(false)
    }

    private void updateButtons() {
        if (hostTextField.text.isEmpty()) {
            testConnectionButton.setEnabled(false)
        } else {
            testConnectionButton.setEnabled(true)
        }

        if (!hostTextField.text.trim().isEmpty() && !nameTextField.text.trim().isEmpty()) {
            if (environmentList.selectedValue != null) {
                def env = environmentList.getModel().getElementAt(environmentList.getSelectedIndex())
                if (env.environmentName != nameTextField.text
                        || env.apiHost != hostTextField.text
                        || env.apiPort != (portSpinner.value as int)
                        || env.apiSslEnabled != sslEnabledCheckbox.isSelected()
                        || env.apiKey != apiTokenTextField.text) {
                    def existingEnvironment = SourcePluginConfig.current.getEnvironment(nameTextField.text)
                    if (existingEnvironment != null && !existingEnvironment.embedded) {
                        saveButton.setEnabled(true)
                    }
                }
            } else {
                saveButton.setEnabled(true)
            }
        }
    }

    @Override
    JPanel getContentPane() {
        return contentPane
    }

    void setData(@NotNull SourcePluginConfig config) {
        if (!config.environments.isEmpty()) {
            DefaultListModel<SourceEnvironmentConfig> model = environmentList.getModel()
            config.environments.each { model.addElement(it) }
            activeEnvironment = SourcePluginConfig.current.activeEnvironment
        }
    }

    void getData(@NotNull SourcePluginConfig data) {
        data.environments = (environmentList.model.collect().flatten() as List<SourceEnvironmentConfig>)
        if (activeEnvironment != null) {
            data.activeEnvironment = activeEnvironment
        } else if (data.activeEnvironment == null && !data.environments.isEmpty()) {
            data.activeEnvironment = data.environments.get(0)
        } else {
            data.activeEnvironment = null
        }
    }

    boolean isModified(@NotNull SourcePluginConfig data) {
        return data.environments != (environmentList.model.collect().flatten() as List<SourceEnvironmentConfig>) ||
                (activeEnvironment != null && SourcePluginConfig.current.activeEnvironment != activeEnvironment)
    }

    void setDataCustom(@NotNull SourcePluginConfig settings) {
        setData(settings)
    }
}
