package com.sourceplusplus.plugin.intellij.settings.connect

import com.sourceplusplus.api.client.SourceCoreClient
import io.gitsocratic.api.SocraticAPI

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

    EnvironmentDialog() {
        setContentPane(contentPane)
        setModal(true)
        portSpinner.model = new SpinnerNumberModel(8080, 0, 65535, 1)
        portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"))

//        if (SourcePluginConfig.current.sppUrl) {
//            def hostUrl = SourcePluginConfig.current.sppUrl
//            if (SourcePluginConfig.current.apiSslEnabled && hostUrl.endsWith(":443")) {
//                hostUrl = hostUrl.substring(0, hostUrl.length() - 4)
//            } else if (!SourcePluginConfig.current.apiSslEnabled && hostUrl.endsWith(":80")) {
//                hostUrl = hostUrl.substring(0, hostUrl.length() - 3)
//            }
//            nameTextField.setText(hostUrl)
//        }
//        if (SourcePluginConfig.current.apiKey) {
//            tokenTextField.setText(SourcePluginConfig.current.apiKey)
//        }
        createButton.addActionListener({
            nameTextField.setEnabled(true)
            hostTextField.setEnabled(true)
            portSpinner.setEnabled(true)
            apiTokenTextField.setEnabled(true)

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

    private void updateButtons() {
        if (hostTextField.text.isEmpty()) {
            testConnectionButton.setEnabled(false)
        } else {
            testConnectionButton.setEnabled(true)
        }

        if (!hostTextField.text.trim().isEmpty() && !nameTextField.text.trim().isEmpty()) {
            saveButton.setEnabled(true)
        }
    }

    @Override
    JPanel getContentPane() {
        return contentPane
    }
}
