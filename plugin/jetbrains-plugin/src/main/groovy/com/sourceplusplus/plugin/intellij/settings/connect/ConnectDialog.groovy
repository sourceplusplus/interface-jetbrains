package com.sourceplusplus.plugin.intellij.settings.connect

import com.intellij.ide.BrowserUtil
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.config.SourcePluginConfig

import javax.swing.*

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.3
 * @since 0.1.0
 */
class ConnectDialog extends JDialog {

    private JPanel contentPane
    private JTextField hostTextField
    private JTextField tokenTextField
    private JButton testConnectionButton
    private JButton purchaseButton

    ConnectDialog() {
        setContentPane(contentPane)
        setModal(true)

        if (SourcePluginConfig.current.sppUrl) {
            def hostUrl = SourcePluginConfig.current.sppUrl
            if (SourcePluginConfig.current.apiSslEnabled && hostUrl.endsWith(":443")) {
                hostUrl = hostUrl.substring(0, hostUrl.length() - 4)
            } else if (!SourcePluginConfig.current.apiSslEnabled && hostUrl.endsWith(":80")) {
                hostUrl = hostUrl.substring(0, hostUrl.length() - 3)
            }
            hostTextField.setText(hostUrl)
        }
        if (SourcePluginConfig.current.apiKey) {
            tokenTextField.setText(SourcePluginConfig.current.apiKey)
        }

        purchaseButton.addActionListener({
            BrowserUtil.browse("https://sourceplusplus.com")
        })
        testConnectionButton.addActionListener({
            def host = hostTextField.getText()
            if (!host.startsWith("http")) {
                host = "http:" + host
            }
            def coreClient = new SourceCoreClient(host)
            if (!tokenTextField.getText().isAllWhitespace()) {
                coreClient.apiKey = tokenTextField.getText().trim()
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
    }

    @Override
    JPanel getContentPane() {
        return contentPane
    }

    String getHost() {
        return hostTextField.getText()
    }

    String getApiToken() {
        return tokenTextField.getText()
    }
}
