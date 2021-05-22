package com.sourceplusplus.sourcemarker.settings;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private JTextField serviceHostTextField;
    private JTextField accessTokenTextField;
    private JPanel testPanel;
    private SourceMarkerConfig config;
    private final SpinnerNumberModel portalRefreshModel;
    private CertificatePinPanel myCertificatePins;

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
        if (!Objects.equals(accessTokenTextField.getText(), config.getAccessToken())) {
            return true;
        }
        if (!Arrays.equals(myCertificatePins.listModel.toArray(), config.getCertificatePins().toArray())) {
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
                accessTokenTextField.getText(),
                new ArrayList<>(Collections.list(myCertificatePins.listModel.elements())),
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
        accessTokenTextField.setText(config.getAccessToken());

        myCertificatePins = new CertificatePinPanel();
        myCertificatePins.listModel.addAll(config.getCertificatePins());
        testPanel.add(myCertificatePins);
    }

    class CertificatePinPanel extends JPanel {
        final DefaultListModel<String> listModel = new DefaultListModel<>();
        final JBList<String> myList = new JBList<>(listModel);

        CertificatePinPanel() {
            setLayout(new BorderLayout());
            myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            myList.setBorder(JBUI.Borders.empty());

            ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myList)
                    .setScrollPaneBorder(JBUI.Borders.empty())
                    .setPanelBorder(JBUI.Borders.customLine(JBColor.border(), 1, 1, 0, 1))
                    .setAddAction(__ -> editCertificatePin(null))
                    .setEditAction(__ -> editCertificatePin())
                    .setRemoveAction(__ -> removeCertificatePin())
                    .disableUpDownActions();

            add(decorator.createPanel(), BorderLayout.EAST);
            JScrollPane scrollPane = new JBScrollPane(myList);
            add(scrollPane, BorderLayout.CENTER);

            scrollPane.setPreferredSize(new Dimension(100, 150));
            scrollPane.setMaximumSize(new Dimension(100, 150));
        }

        void select(String pattern) {
            ScrollingUtil.selectItem(myList, pattern);
        }

        void removeSelected() {
            String selectedValue = getSelectedItem();
            if (selectedValue == null) return;
            ListUtil.removeSelectedItems(myList);
        }

        String getSelectedItem() {
            return myList.getSelectedValue();
        }
    }

    private void editCertificatePin() {
        String item = myCertificatePins.getSelectedItem();
        if (item == null) return;
        editCertificatePin(item);
    }

    private void editCertificatePin(@Nullable("null means new") String oldPin) {
        Project activeProject = ProjectManager.getInstance().getOpenProjects()[0];
        String pin = Messages.showInputDialog(activeProject,
                "Certificate SHA-256 Fingerprint", "Certificate Pin", null, oldPin, null);
        if (pin != null && !pin.isEmpty()) {
            if (oldPin != null) {
                myCertificatePins.listModel.removeElement(oldPin);
            }
            myCertificatePins.listModel.add(0, pin);
            myCertificatePins.select(pin);
        }
    }

    private void removeCertificatePin() {
        myCertificatePins.removeSelected();
    }
}
