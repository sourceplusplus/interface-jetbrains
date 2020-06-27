package com.sourceplusplus.plugin.intellij.settings.application

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.ProjectScope
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity

import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

/**
 * Used to create a new application or pick an existing application to associate with the current project.
 *
 * @version 0.3.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ApplicationSettingsDialog extends JDialog {

    private JPanel contentPane
    private JTextField applicationNameTextField
    private JTextField applicationDomainTextField
    private JComboBox<ApplicationChoice> existingApplicationsComboBox
    private GhostText ghostText

    ApplicationSettingsDialog() {
        setContentPane(contentPane)
        setModal(true)
        existingApplicationsComboBox.addItem(new ApplicationChoice(null))

        PsiPackage[] basePackages = JavaPsiFacade.getInstance(IntelliJStartupActivity.currentProject).findPackage("")
                ?.getSubPackages(ProjectScope.getProjectScope(IntelliJStartupActivity.currentProject))

        //remove non-code packages
        basePackages = basePackages?.findAll { it.qualifiedName != "asciidoc" }?.toArray(new PsiPackage[0])

        //determine deepest common source package
        if (basePackages) {
            def rootPackage = null
            while (basePackages.length == 1) {
                rootPackage = basePackages[0].qualifiedName
                basePackages = basePackages[0].getSubPackages(ProjectScope.getProjectScope(IntelliJStartupActivity.currentProject))
            }
            if (rootPackage != null) {
                def defaultApplicationDomain = rootPackage.replace(".", "/") + "/.*"
                ghostText = new GhostText(applicationDomainTextField, defaultApplicationDomain)
            }
        }
    }

    @Override
    JPanel getContentPane() {
        return contentPane
    }

    void addExistingApplication(SourceApplication... applications) {
        for (SourceApplication application : applications) {
            existingApplicationsComboBox.addItem(new ApplicationChoice(application))
        }
    }

    String getApplicationName() {
        return applicationNameTextField.getText()
    }

    String getApplicationDomain() {
        if (ghostText != null && ghostText.isEffectivelyEmpty()) {
            return ghostText.ghostText
        }
        return applicationDomainTextField.getText()
    }

    SourceApplication getExistingApplication() {
        return ((ApplicationChoice) Objects.requireNonNull(existingApplicationsComboBox.getSelectedItem())).application
    }

    private static final class ApplicationChoice {
        final SourceApplication application

        ApplicationChoice(SourceApplication application) {
            this.application = application
        }

        @Override
        String toString() {
            return application == null ? "" : application.appName()
        }
    }

    private static class GhostText implements FocusListener, DocumentListener, PropertyChangeListener {
        private final JTextField textfield
        private final String ghostText
        private Color ghostColor
        private Color foregroundColor
        private boolean isEmpty

        GhostText(final JTextField textfield, String ghostText) {
            this.textfield = textfield
            this.ghostText = ghostText
            this.ghostColor = Color.GRAY
            textfield.addFocusListener(this)
            registerListeners()
            updateState()
            if (!this.textfield.hasFocus()) {
                focusLost(null)
            }
        }

        boolean isEffectivelyEmpty() {
            return isEmpty
        }

        String getGhostText() {
            return ghostText
        }

        private void registerListeners() {
            textfield.getDocument().addDocumentListener(this)
            textfield.addPropertyChangeListener("foreground", this)
        }

        private void unregisterListeners() {
            textfield.getDocument().removeDocumentListener(this)
            textfield.removePropertyChangeListener("foreground", this)
        }

        private void updateState() {
            isEmpty = textfield.getText().length() == 0
            foregroundColor = textfield.getForeground()
        }

        @Override
        void focusGained(FocusEvent e) {
            if (isEmpty) {
                unregisterListeners()
                try {
                    textfield.setText("")
                    textfield.setForeground(foregroundColor)
                } finally {
                    registerListeners()
                }
            }
        }

        @Override
        void focusLost(FocusEvent e) {
            if (isEmpty) {
                unregisterListeners()
                try {
                    textfield.setText(ghostText)
                    textfield.setForeground(ghostColor)
                } finally {
                    registerListeners()
                }
            }
        }

        @Override
        void propertyChange(PropertyChangeEvent evt) {
            updateState()
        }

        @Override
        void changedUpdate(DocumentEvent e) {
            updateState()
        }

        @Override
        void insertUpdate(DocumentEvent e) {
            updateState()
        }

        @Override
        void removeUpdate(DocumentEvent e) {
            updateState()
        }
    }
}
