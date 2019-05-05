package com.sourceplusplus.plugin.intellij.settings.connect

import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.config.SourceEnvironmentConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import io.gitsocratic.api.SocraticAPI
import org.jetbrains.annotations.NotNull

import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class EnvironmentDialog extends JDialog {

    private JPanel contentPane
    private JList environmentList
    private JButton createButton
    private JButton deleteButton
    private JButton setupViaDockerButton
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
            clearConnectionForm(true)
            nameTextField.requestFocus()
        })
        setupViaDockerButton.addActionListener({
            def connectDialog = new ConnectionInfoDialogWrapper()
            connectDialog.createCenterPanel()

            def input = new PipedInputStream()
            def output = new PipedOutputStream()
            input.connect(output)
            Thread.startDaemon {
                input.eachLine {
                    connectDialog.log(it + "\n")
                }
            }
            Thread.startDaemon {
                connectDialog.setStatus("Initializing Apache Skywalking...")
                def initSkywalking = SocraticAPI.administration().initApacheSkywalking()
                        .build().execute(output)
                if (initSkywalking.status != 0) {
                    connectDialog.setStatus("<font color='red'>Failed to initialize Apache Skywalking service</font>")
                    output.close()
                    input.close()
                    return
                }

                connectDialog.setStatus("Initializing Source++...")
                def initSpp = SocraticAPI.administration().initSourcePlusPlus()
                        .build().execute(output)
                if (initSpp.status != 0) {
                    connectDialog.setStatus("<font color='red'>Failed to initialize Source++ service</font>")
                    output.close()
                    input.close()
                    return
                }
                output.close()
                input.close()
                connectDialog.setStatus("<font color='green'>Successful</font>")
            }
            connectDialog.show()
        })

        environmentList.addListSelectionListener({
            if (environmentList.selectedValue != null) {
                deleteButton.setEnabled(true)
                clearConnectionForm(true)
                nameTextField.requestFocus()

                def env = environmentList.selectedValue as SourceEnvironmentConfig
                nameTextField.text = env.environmentName
                hostTextField.text = env.apiHost
                portSpinner.value = env.apiPort
                apiTokenTextField.text = env.apiKey
                sslEnabledCheckbox.setSelected(env.apiSslEnabled)

                activateButton.setEnabled(SourcePluginConfig.current.activeEnvironment != env)
            } else {
                deleteButton.setEnabled(false)
                activateButton.setEnabled(false)
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
                DefaultListModel<SourceEnvironmentConfig> model = environmentList.getModel()
                activeEnvironment = model.getElementAt(environmentList.getSelectedIndex())
                activateButton.setEnabled(false)
            }
        })
        saveButton.addActionListener({
            def env = new SourceEnvironmentConfig()
            env.environmentName = nameTextField.text
            env.apiHost = hostTextField.text
            env.apiPort = portSpinner.value as int
            env.apiSslEnabled = sslEnabledCheckbox.isSelected()
            env.apiKey = apiTokenTextField.text
            clearConnectionForm(false)
            (environmentList.model as DefaultListModel<SourceEnvironmentConfig>).addElement(env)
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
                DefaultListModel<SourceEnvironmentConfig> model = environmentList.getModel()
                def env = model.getElementAt(environmentList.getSelectedIndex())
                if (env.environmentName != nameTextField.text
                        || env.apiHost != hostTextField.text
                        || env.apiPort != (portSpinner.value as int)
                        || env.apiSslEnabled != sslEnabledCheckbox.isSelected()
                        || env.apiKey != apiTokenTextField.text) {
                    saveButton.setEnabled(true)
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
