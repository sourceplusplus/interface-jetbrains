package com.sourceplusplus.plugin.intellij.settings.connect

import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.config.SourceEnvironmentConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.integration.IntegrationConnection
import com.sourceplusplus.api.model.integration.IntegrationInfo
import io.gitsocratic.api.SocraticAPI
import io.gitsocratic.command.config.ConfigOption
import io.gitsocratic.command.result.InitDockerCommandResult
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
            def connectDialog = new ConnectionInfoDialogWrapper("Docker Setup")
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
                if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                    ConfigOption.docker_host.setValue("192.168.99.100")
                }
                connectDialog.setStatus("Initializing Apache SkyWalking...")
                def initSkywalking = SocraticAPI.administration().initApacheSkyWalking()
                        .build().execute(output) as InitDockerCommandResult
                if (initSkywalking.status != 0) {
                    connectDialog.setStatus("<font color='red'>Failed to initialize Apache SkyWalking service</font>")
                    output.close()
                    input.close()
                    return
                }
                Thread.sleep(10000) //todo: better (waits for skywalking to boot)

                connectDialog.setStatus("Initializing Source++...")
                def initSpp = SocraticAPI.administration().initSourcePlusPlus()
                        .link("Apache_SkyWalking")
                        .build().execute(output) as InitDockerCommandResult
                if (initSpp.status != 0) {
                    connectDialog.setStatus("<font color='red'>Failed to initialize Source++ service</font>")
                    output.close()
                    input.close()
                    return
                }
                output.close()
                input.close()

                connectDialog.setStatus("Integrating Apache SkyWalking with Source++...")
                def skywalkingBinding = initSkywalking.portBindings.get("12800/tcp")[0]
                def skywalkingPort = skywalkingBinding.substring(skywalkingBinding.indexOf(":") + 1) as int
                def sppBinding = initSpp.portBindings.get("8080/tcp")[0]
                def sppHost = sppBinding.substring(0, sppBinding.indexOf(":"))
                def sppPort = sppBinding.substring(sppBinding.indexOf(":") + 1) as int
                if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                    sppHost = "192.168.99.100"
                }

                Thread.sleep(10000) //todo: better (waits for spp to boot)
                def coreClient = new SourceCoreClient(sppHost, sppPort, sslEnabledCheckbox.isSelected())
                def integrationInfo = IntegrationInfo.builder()
                        .id("apache_skywalking")
                        .enabled(true)
                        .connection(IntegrationConnection.builder().host("Apache_SkyWalking").port(skywalkingPort).build())
                        .build()
                coreClient.updateIntegrationInfo(integrationInfo, {
                    if (it.succeeded()) {
                        def env = new SourceEnvironmentConfig()
                        env.environmentName = "Docker"
                        env.apiHost = sppHost
                        env.apiPort = sppPort
                        env.apiSslEnabled = false
                        clearConnectionForm(false)
                        (environmentList.model as DefaultListModel<SourceEnvironmentConfig>).addElement(env)
                        connectDialog.setStatus("<font color='green'>Successful</font>")
                    } else {
                        connectDialog.setError(it.cause())
                    }
                })
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
            def addIndex = 0
            if (environmentList.selectedValue != null) {
                //update environment
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
