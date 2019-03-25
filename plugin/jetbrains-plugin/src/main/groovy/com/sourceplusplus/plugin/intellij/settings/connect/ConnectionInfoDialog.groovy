package com.sourceplusplus.plugin.intellij.settings.connect

import com.sourceplusplus.api.model.info.SourceCoreInfo
import io.vertx.core.json.Json

import javax.swing.*

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.3
 * @since 0.1.0
 */
class ConnectionInfoDialog extends JDialog {

    private JPanel contentPane
    private JPanel mainPanel
    private JLabel connectionStatusLabel
    private JScrollPane connectionInfoScrollPane
    private JTextArea connectionInfoTextArea

    ConnectionInfoDialog() {
        setContentPane(contentPane)
        setModal(true)
    }

    void setSuccessful(SourceCoreInfo sourceCoreInfo) {
        connectionStatusLabel.setText("<html><b>Connection Status: <font color='green'>Successful</font></b></html>")
        connectionInfoTextArea.append(Json.encodePrettily(sourceCoreInfo))
        connectionInfoTextArea.setCaretPosition(0)
    }

    void setError(Throwable ex) {
        connectionStatusLabel.setText("<html><b>Connection Status: <font color='red'>Failed</font></b></html>")
        connectionInfoTextArea.append(ex.getMessage() + "\n")
        for (def el : ex.getStackTrace()) {
            connectionInfoTextArea.append(el.toString() + "\n")
        }
        connectionInfoTextArea.setCaretPosition(0)
    }
}
