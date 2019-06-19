package com.sourceplusplus.plugin.intellij.settings.about;

import javax.swing.*;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
public class AboutDialog extends JDialog {

    private JPanel contentPane;
    private JLabel websiteLabel;
    private JLabel knowledgeBaseLabel;
    private JLabel supportLabel;
    private JLabel versionLabel;

    public AboutDialog() {
        setContentPane(contentPane);
        setModal(true);
    }

    @Override
    public JPanel getContentPane() {
        return contentPane;
    }

    public JLabel getWebsiteLabel() {
        return websiteLabel;
    }

    public JLabel getKnowledgeBaseLabel() {
        return knowledgeBaseLabel;
    }

    public JLabel getSupportLabel() {
        return supportLabel;
    }

    public JLabel getVersionLabel() {
        return versionLabel;
    }
}
