package com.sourceplusplus.plugin.intellij.settings.connect

import javax.swing.*

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
    private JTextField hostTextField
    private JTextField apiTokenTextField
    private JButton activateButton

    EnvironmentDialog() {
        setContentPane(contentPane)
        setModal(true)
    }

    @Override
    JPanel getContentPane() {
        return contentPane
    }
}
