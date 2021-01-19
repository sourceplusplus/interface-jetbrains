package com.sourceplusplus.sourcemarker.settings;

import com.intellij.ui.IdeBorderFactory;

import javax.swing.*;
import java.util.Objects;

import static com.sourceplusplus.sourcemarker.PluginBundle.message;

public class PluginConfigurationPanel {
    private JPanel myWholePanel;
    private JPanel myProjectSettingsPanel;
    private JPanel myGlobalSettingsPanel;
    private JTextField skywalkingOapTextField;
    private JTextField rootSourcePackageTextField;
    private JComboBox<String> skywalkingVersionComboBox;
    private SourceMarkerConfig config;

    public PluginConfigurationPanel() {
        myProjectSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("apache_skywalking_settings")));
        myGlobalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("plugin_settings")));
    }

    public JComponent getContentPane() {
        return myWholePanel;
    }

    boolean isModified() {
        if (!Objects.equals(skywalkingOapTextField.getText(), config.getSkywalkingOapUrl())) {
            return true;
        }
        if (!Objects.equals(rootSourcePackageTextField.getText(), config.getRootSourcePackage())) {
            return true;
        }
        return false;
    }

    public SourceMarkerConfig getPluginConfig() {
        return new SourceMarkerConfig(
                skywalkingOapTextField.getText(),
                rootSourcePackageTextField.getText()
        );
    }

    public void applySourceMarkerConfig(SourceMarkerConfig config) {
        this.config = config;
        skywalkingOapTextField.setText(config.getSkywalkingOapUrl());
        rootSourcePackageTextField.setText(config.getRootSourcePackage());
    }
}
