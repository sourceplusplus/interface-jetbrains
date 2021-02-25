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
    private JSpinner portalRefreshSpinner;
    private JCheckBox consoleCheckBox;
    private SourceMarkerConfig config;
    private final SpinnerNumberModel portalRefreshModel;

    public PluginConfigurationPanel() {
        myProjectSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("apache_skywalking_settings")));
        myGlobalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("plugin_settings")));
        portalRefreshModel = new SpinnerNumberModel();
        portalRefreshModel.setMinimum(0);
        portalRefreshSpinner.setModel(portalRefreshModel);
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
        if (!Objects.equals(portalRefreshModel.getNumber().intValue(), config.getPortalRefreshIntervalMs())) {
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
                portalRefreshModel.getNumber().intValue(),
                true
        );
    }

    public void applySourceMarkerConfig(SourceMarkerConfig config) {
        this.config = config;
        skywalkingOapTextField.setText(config.getSkywalkingOapUrl());
        rootSourcePackageTextField.setText(config.getRootSourcePackage());
        autoResolveEndpointNamesCheckBox.setSelected(config.getAutoResolveEndpointNames());
        consoleCheckBox.setSelected(config.getPluginConsoleEnabled());
        portalRefreshModel.setValue(config.getPortalRefreshIntervalMs());
    }
}
