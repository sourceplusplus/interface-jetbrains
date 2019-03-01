package com.sourceplusplus.plugin.intellij.settings.about

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.sourceplusplus.plugin.SourcePluginDefines
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.swing.*
import java.awt.*

/**
 * todo: description
 *
 * @version 0.1.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class AboutConfigurable implements Configurable {

    private AboutDialog form

    @Nls
    @NotNull
    @Override
    String getDisplayName() {
        return "About"
    }

    @NonNls
    @Nullable
    @Override
    String getHelpTopic() {
        return null
    }

    @NotNull
    @Override
    JComponent createComponent() {
        if (form == null) {
            form = new AboutDialog()
        }

        form.versionLabel.setText("Version " + SourcePluginDefines.VERSION + ", © 2019 CodeBrig, LLC.")
        form.websiteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR))
        form.websiteLabel.addMouseListener(new HyperlinkMouseListener(form.websiteLabel,
                "https://sourceplusplus.com"))
        form.knowledgeBaseLabel.setCursor(new Cursor(Cursor.HAND_CURSOR))
        form.knowledgeBaseLabel.addMouseListener(new HyperlinkMouseListener(form.knowledgeBaseLabel,
                "https://sourceplusplus.com/kb"))
        form.supportLabel.setCursor(new Cursor(Cursor.HAND_CURSOR))
        form.supportLabel.addMouseListener(new HyperlinkMouseListener(form.supportLabel,
                "mailto:support@sourceplusplus.com"))

        return form.getContentPane()
    }

    @Override
    boolean isModified() {
        return false
    }

    @Override
    void apply() throws ConfigurationException {
    }

    @Override
    void reset() {
    }

    @Override
    void disposeUIResources() {
        form = null
    }
}