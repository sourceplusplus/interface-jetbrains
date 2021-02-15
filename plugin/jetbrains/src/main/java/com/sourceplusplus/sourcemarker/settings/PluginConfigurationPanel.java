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
    private JCheckBox autoResolveEndpointNamesCheckBox;
    private JSpinner spinner1;
    private JCheckBox consoleCheckBox;
    private SourceMarkerConfig config;
    private final SpinnerNumberModel numberModel;

    public PluginConfigurationPanel() {
        myProjectSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("apache_skywalking_settings")));
        myGlobalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("plugin_settings")));
        numberModel = new SpinnerNumberModel();
        numberModel.setMinimum(0);
        spinner1.setModel(numberModel);
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
        if (!Objects.equals(autoResolveEndpointNamesCheckBox.isSelected(), config.getAutoResolveEndpointNames())) {
            return true;
        }
        if (!Objects.equals(numberModel.getNumber().intValue(), config.getPortalRefreshIntervalMs())) {
            return true;
        }
        if (!Objects.equals(consoleCheckBox.isSelected(), config.getPluginConsoleEnabled())) {
            return true;
        }
        return false;
    }

    public SourceMarkerConfig getPluginConfig() {
        return new SourceMarkerConfig(
                skywalkingOapTextField.getText(),
                rootSourcePackageTextField.getText(),
                autoResolveEndpointNamesCheckBox.isSelected(),
                true, consoleCheckBox.isSelected(),
                numberModel.getNumber().intValue()
        );
    }

    public void applySourceMarkerConfig(SourceMarkerConfig config) {
        this.config = config;
        skywalkingOapTextField.setText(config.getSkywalkingOapUrl());
        rootSourcePackageTextField.setText(config.getRootSourcePackage());
        autoResolveEndpointNamesCheckBox.setSelected(config.getAutoResolveEndpointNames());
        consoleCheckBox.setSelected(config.getPluginConsoleEnabled());
        numberModel.setValue(config.getPortalRefreshIntervalMs());
    }
}
