package com.sourceplusplus.plugin.intellij.settings.application;

import com.sourceplusplus.api.model.application.SourceApplication;

import javax.swing.*;
import java.util.Objects;

public class ApplicationSettingsDialog extends JDialog {

    private JPanel contentPane;
    private JTextField applicationNameTextField;
    private JTextField applicationDomainTextField;
    private JComboBox existingApplicationsComboBox;

    public ApplicationSettingsDialog() {
        setContentPane(contentPane);
        setModal(true);
        existingApplicationsComboBox.addItem(new ApplicationChoice(null));
    }

    @Override
    public JPanel getContentPane() {
        return contentPane;
    }

    @SuppressWarnings("unchecked")
    public void addExistingApplication(SourceApplication... applications) {
        for (SourceApplication application : applications) {
            existingApplicationsComboBox.addItem(new ApplicationChoice(application));
        }
    }

    public String getApplicationName() {
        return applicationNameTextField.getText();
    }

    public String getApplicationDomain() {
        return applicationDomainTextField.getText();
    }

    public SourceApplication getExistingApplication() {
        return ((ApplicationChoice) Objects.requireNonNull(existingApplicationsComboBox.getSelectedItem())).application;
    }

    public static final class ApplicationChoice {
        SourceApplication application;

        ApplicationChoice(SourceApplication application) {
            this.application = application;
        }

        @Override
        public String toString() {
            return application == null ? "" : application.appName();
        }
    }
}
