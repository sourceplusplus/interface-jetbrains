package com.sourceplusplus.plugin.intellij.settings.connect

import com.sourceplusplus.api.APIException
import com.sourceplusplus.api.model.info.SourceCoreInfo
import io.vertx.core.json.Json

import javax.swing.*

/**
 * Displays core connection information.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
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

    void log(String data) {
        connectionInfoTextArea.text += data
        connectionInfoTextArea.setCaretPosition(connectionInfoTextArea.document.length)
    }

    void setStatus(String status) {
        connectionStatusLabel.setText("<html><b>Connection Status: $status</b></html>")
    }

    void setSuccessful(SourceCoreInfo sourceCoreInfo) {
        connectionStatusLabel.setText("<html><b>Connection Status: <font color='green'>Successful</font></b></html>")
        connectionInfoTextArea.append(Json.encodePrettily(sourceCoreInfo))
        connectionInfoTextArea.setCaretPosition(0)
    }

    void setError(Throwable ex) {
        connectionStatusLabel.setText("<html><b>Connection Status: <font color='red'>Failed</font></b></html>")
        if (ex instanceof APIException) {
            if (ex.isUnauthorizedAccess()) {
                connectionInfoTextArea.append("Server responded with an unauthorized access error.\n" +
                        "You do not have permission to access Source++ Core with the API token you provided.")
            } else {
                connectionInfoTextArea.append(ex.getMessage() + "\n")
                for (def el : ex.getStackTrace()) {
                    connectionInfoTextArea.append(el.toString() + "\n")
                }
            }
        } else {
            connectionInfoTextArea.append(ex.getMessage() + "\n")
            for (def el : ex.getStackTrace()) {
                connectionInfoTextArea.append(el.toString() + "\n")
            }
        }
        connectionInfoTextArea.setCaretPosition(0)
    }
}
