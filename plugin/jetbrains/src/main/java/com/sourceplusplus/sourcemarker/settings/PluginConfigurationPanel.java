package com.sourceplusplus.sourcemarker.settings;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intellij.ui.IdeBorderFactory;
import io.vertx.core.json.JsonObject;

import javax.swing.*;
import java.io.IOException;
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
    private JPanel myServiceSettingsPanel;
    private JTextArea serviceCertificateTextArea;
    private JTextField serviceHostTextField;
    private SourceMarkerConfig config;
    private final SpinnerNumberModel portalRefreshModel;

    public PluginConfigurationPanel() {
        JsonObject configuration;
        try {
            configuration = new JsonObject(Resources.toString(Resources.getResource(getClass(),
                    "/plugin-configuration.json"), Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (configuration.getJsonObject("visible_settings").getBoolean("apache_skywalking")) {
            myProjectSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("apache_skywalking_settings")));
        } else {
            myProjectSettingsPanel.setVisible(false);
        }
        if (configuration.getJsonObject("visible_settings").getBoolean("service_discovery")) {
            myServiceSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("service_settings")));
        } else {
            myServiceSettingsPanel.setVisible(false);
        }
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
        if (!Objects.equals(serviceHostTextField.getText(), config.getServiceHost())) {
            return true;
        }
        if (!Objects.equals(serviceCertificateTextArea.getText(), config.getServiceCertificate())) {
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
                serviceHostTextField.getText(),
                serviceCertificateTextArea.getText(),
                null
        );
    }

    public void applySourceMarkerConfig(SourceMarkerConfig config) {
        this.config = config;
        skywalkingOapTextField.setText(config.getSkywalkingOapUrl());
        rootSourcePackageTextField.setText(config.getRootSourcePackage());
        autoResolveEndpointNamesCheckBox.setSelected(config.getAutoResolveEndpointNames());
        consoleCheckBox.setSelected(config.getPluginConsoleEnabled());
        portalRefreshModel.setValue(config.getPortalRefreshIntervalMs());
        serviceHostTextField.setText(config.getServiceHost());
        serviceCertificateTextArea.setText(config.getServiceCertificate());
    }
}
