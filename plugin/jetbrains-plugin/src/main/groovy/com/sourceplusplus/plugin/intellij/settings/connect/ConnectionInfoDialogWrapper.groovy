package com.sourceplusplus.plugin.intellij.settings.connect

import com.intellij.openapi.ui.DialogWrapper
import com.sourceplusplus.api.model.info.SourceCoreInfo
import org.jetbrains.annotations.Nullable

import javax.swing.*

/**
 * todo: description
 *
 * @version 0.1.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ConnectionInfoDialogWrapper extends DialogWrapper {

    private final ConnectionInfoDialog connectionInfoDialog = new ConnectionInfoDialog()

    ConnectionInfoDialogWrapper(SourceCoreInfo coreInfo) {
        super(false)
        init()
        setTitle("Connection Info")
        connectionInfoDialog.setSuccessful(coreInfo)
    }

    ConnectionInfoDialogWrapper(Throwable ex) {
        super(false)
        init()
        setTitle("Connection Info")
        connectionInfoDialog.setError(ex)
    }

    @Nullable
    @Override
    JComponent createCenterPanel() {
        return connectionInfoDialog.getContentPane() as JComponent
    }

    @Override
    protected void doOKAction() {
        super.doOKAction()
    }

    @Override
    protected Action[] createActions() {
        return [getOKAction()]
    }
}
