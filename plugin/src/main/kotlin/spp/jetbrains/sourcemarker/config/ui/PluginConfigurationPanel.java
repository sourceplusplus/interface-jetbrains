/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.config.ui;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;
import spp.jetbrains.sourcemarker.config.PortalConfig;
import spp.jetbrains.sourcemarker.config.SourceMarkerConfig;
import spp.protocol.platform.general.Service;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static spp.jetbrains.PluginBundle.message;

public class PluginConfigurationPanel {
    private JPanel myWholePanel;
    private JPanel myGlobalSettingsPanel;
    private JCheckBox autoResolveEndpointNamesCheckBox;
    private JPanel myServiceSettingsPanel;
    private JBTextField serviceHostTextField;
    private JBTextField authorizationCodeTextField;
    private JPanel testPanel;
    private ComboBox<String> serviceComboBox;
    private JBCheckBox verifyHostCheckBox;
    private JBLabel verifyHostLabel;
    private JBLabel hostLabel;
    private JBLabel authorizationCodeLabel;
    private JBLabel certificatePinsLabel;
    private JBLabel serviceLabel;
    private JPanel myPortalSettingsPanel;
    private JSpinner portalZoomSpinner;
    private SourceMarkerConfig config;
    private CertificatePinPanel myCertificatePins;

    public PluginConfigurationPanel(SourceMarkerConfig config, List<Service> availableServices) {
        this.config = config;
        myServiceSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("service_settings")));
        myGlobalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("plugin_settings")));
        myPortalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("portal_settings")));
        portalZoomSpinner.setModel(new SpinnerNumberModel(1.0, 0.5, 2.0, 0.1));

        availableServices.forEach(service -> serviceComboBox.addItem(service.getName()));
        if (config.getServiceName() != null) {
            if (availableServices.stream().noneMatch(service -> service.getName().equals(config.getServiceName()))) {
                serviceComboBox.addItem(config.getServiceName());
            }
            serviceComboBox.setSelectedItem(config.getServiceName());
        }

        //todo: shouldn't need to manually update locale text
        hostLabel.setText(message("service_host"));
        authorizationCodeLabel.setText(message("authorization_code"));
        verifyHostLabel.setText(message("verify_host"));
        certificatePinsLabel.setText(message("certificate_pins"));
        serviceLabel.setText(message("service"));
        autoResolveEndpointNamesCheckBox.setText(message("auto_resolve_endpoint_names"));
    }

    private void setUIEnabled(boolean enabled) {
        autoResolveEndpointNamesCheckBox.setEnabled(enabled);
        serviceHostTextField.setEnabled(enabled);
        authorizationCodeTextField.setEnabled(enabled);
        serviceComboBox.setEnabled(enabled);
        verifyHostCheckBox.setEnabled(enabled);
        portalZoomSpinner.setEnabled(enabled);
    }

    public JComponent getContentPane() {
        return myWholePanel;
    }

    public boolean isModified() {
        if (config.getOverride()) return false;

        if (!Objects.equals(autoResolveEndpointNamesCheckBox.isSelected(), config.getAutoResolveEndpointNames())) {
            return true;
        }
        if (!Objects.equals(serviceHostTextField.getText(), config.getServiceHost() != null ? config.getServiceHost() : "")) {
            return true;
        }
        if (!Objects.equals(authorizationCodeTextField.getText(), config.getAuthorizationCode() != null ? config.getAuthorizationCode() : "")) {
            return true;
        }
        if (!Arrays.equals(myCertificatePins.listModel.toArray(), config.getCertificatePins().toArray())) {
            return true;
        }
        if (!Objects.equals(verifyHostCheckBox.isSelected(), config.getVerifyHost())) {
            return true;
        }
        if (!Objects.equals(serviceComboBox.getSelectedItem(), config.getServiceName()) &&
                !(config.getServiceName() == null && Objects.equals(serviceComboBox.getSelectedItem(), "All Services"))) {
            return true;
        }
        if (!Objects.equals(portalZoomSpinner.getValue(), config.getPortalConfig().getZoomLevel())) {
            return true;
        }
        return false;
    }

    public SourceMarkerConfig getPluginConfig() {
        String currentService = serviceComboBox.getSelectedItem().toString();
        if ("All Services".equals(currentService)) {
            currentService = null;
        }

        return new SourceMarkerConfig(
                autoResolveEndpointNamesCheckBox.isSelected(),
                true,
                serviceHostTextField.getText(),
                authorizationCodeTextField.getText(),
                new ArrayList<>(Collections.list(myCertificatePins.listModel.elements())),
                null,
                verifyHostCheckBox.isSelected(),
                currentService,
                false,
                new PortalConfig((Double) portalZoomSpinner.getValue()),
                new HashMap<>(),
                false
        );
    }

    public void applySourceMarkerConfig(SourceMarkerConfig config) {
        this.config = config;
        autoResolveEndpointNamesCheckBox.setSelected(config.getAutoResolveEndpointNames());
        serviceHostTextField.setText(config.getServiceHost());
        authorizationCodeTextField.setText(config.getAuthorizationCode());
        verifyHostCheckBox.setSelected(config.getVerifyHost());

        myCertificatePins = new CertificatePinPanel(!config.getOverride());
        myCertificatePins.listModel.addAll(config.getCertificatePins());
        testPanel.add(myCertificatePins);

        portalZoomSpinner.setValue(config.getPortalConfig().getZoomLevel());

        setUIEnabled(!config.getOverride());
    }

    class CertificatePinPanel extends JBPanel<CertificatePinPanel> {
        final DefaultListModel<String> listModel = new DefaultListModel<>();
        final JBList<String> myList = new JBList<>(listModel);

        CertificatePinPanel(boolean enabled) {
            setLayout(new BorderLayout());
            myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            myList.setBorder(JBUI.Borders.empty());
            myList.setEnabled(enabled);

            ToolbarDecorator decorator;
            if (enabled) {
                decorator = ToolbarDecorator.createDecorator(myList)
                        .setToolbarPosition(ActionToolbarPosition.RIGHT)
                        .setScrollPaneBorder(JBUI.Borders.empty())
                        .setPanelBorder(JBUI.Borders.empty())
                        .setAddAction(__ -> editCertificatePin(null))
                        .setEditAction(__ -> editCertificatePin())
                        .setRemoveAction(__ -> removeCertificatePin())
                        .disableUpDownActions();
            } else {
                decorator = ToolbarDecorator.createDecorator(myList)
                        .setToolbarPosition(ActionToolbarPosition.RIGHT)
                        .setScrollPaneBorder(JBUI.Borders.empty())
                        .setPanelBorder(JBUI.Borders.empty())
                        .disableAddAction()
                        .disableRemoveAction()
                        .disableUpDownActions();
            }

            add(decorator.createPanel(), BorderLayout.EAST);
            JScrollPane scrollPane = new JBScrollPane(myList);
            add(scrollPane, BorderLayout.CENTER);

            scrollPane.setMinimumSize(new Dimension(100, 0));
            scrollPane.setMaximumSize(new Dimension(100, 0));
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
