package com.sourceplusplus.sourcemarker.settings;

import javax.swing.*;
import java.util.Objects;

public class PluginConfigurationDialog extends JDialog {

    private JPanel contentPane;
    private JTextField skywalkingOapTextField;
    private SourceMarkerConfig config;

    public PluginConfigurationDialog() {
        setContentPane(contentPane);
        setModal(true);
    }

    boolean isModified() {
        if (!Objects.equals(skywalkingOapTextField.getText(), config.getSkywalkingOapUrl())) {
            return true;
        }
        return false;
    }

    public SourceMarkerConfig getPluginConfig() {
        return new SourceMarkerConfig(skywalkingOapTextField.getText());
    }

    public void applySourceMarkerConfig(SourceMarkerConfig config) {
        this.config = config;
        skywalkingOapTextField.setText(config.getSkywalkingOapUrl());
    }
}
