package spp.jetbrains.sourcemarker.settings;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;
import spp.protocol.general.Service;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static spp.jetbrains.sourcemarker.PluginBundle.message;

public class PluginConfigurationPanel {
    private JPanel myWholePanel;
    private JPanel myGlobalSettingsPanel;
    private JTextField rootSourcePackageTextField;
    private JCheckBox autoResolveEndpointNamesCheckBox;
    private JCheckBox consoleCheckBox;
    private JPanel myServiceSettingsPanel;
    private JTextField serviceHostTextField;
    private JTextField accessTokenTextField;
    private JPanel testPanel;
    private JComboBox serviceComboBox;
    private JCheckBox verifyHostCheckBox;
    private JLabel verifyHostLabel;
    private SourceMarkerConfig config;
    private CertificatePinPanel myCertificatePins;

    public PluginConfigurationPanel(SourceMarkerConfig config, java.util.List<Service> services) {
        this.config = config;
        myServiceSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("service_settings")));
        myGlobalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("plugin_settings")));

        services.forEach(service -> serviceComboBox.addItem(service.getName()));
        if (config.getServiceName() != null) {
            serviceComboBox.setSelectedItem(config.getServiceName());
        }
    }

    public JComponent getContentPane() {
        return myWholePanel;
    }

    boolean isModified() {
        if (!Arrays.equals(rootSourcePackageTextField.getText().split(","), config.getRootSourcePackages().toArray())) {
            return true;
        }
        if (!Objects.equals(autoResolveEndpointNamesCheckBox.isSelected(), config.getAutoResolveEndpointNames())) {
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
        if (!Objects.equals(verifyHostCheckBox.isSelected(), config.getVerifyHost())) {
            return true;
        }
        if (!Objects.equals(serviceComboBox.getSelectedItem(), config.getServiceName())) {
            return config.getServiceName() != null || serviceComboBox.getSelectedItem() != "All Services";
        }
        return false;
    }

    public SourceMarkerConfig getPluginConfig() {
        String currentService = serviceComboBox.getSelectedItem().toString();
        if ("All Services".equals(currentService)) {
            currentService = null;
        }

        return new SourceMarkerConfig(
                Arrays.stream(rootSourcePackageTextField.getText().split(","))
                        .map(String::trim).collect(Collectors.toList()),
                autoResolveEndpointNamesCheckBox.isSelected(),
                true, consoleCheckBox.isSelected(),
                serviceHostTextField.getText(),
                accessTokenTextField.getText(),
                new ArrayList<>(Collections.list(myCertificatePins.listModel.elements())),
                null,
                verifyHostCheckBox.isSelected(),
                currentService
        );
    }

    public void applySourceMarkerConfig(SourceMarkerConfig config) {
        this.config = config;
        rootSourcePackageTextField.setText(String.join(",", config.getRootSourcePackages()));
        autoResolveEndpointNamesCheckBox.setSelected(config.getAutoResolveEndpointNames());
        consoleCheckBox.setSelected(config.getPluginConsoleEnabled());
        serviceHostTextField.setText(config.getServiceHost());
        accessTokenTextField.setText(config.getAccessToken());
        verifyHostCheckBox.setSelected(config.getVerifyHost());

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
                    .setToolbarPosition(ActionToolbarPosition.RIGHT)
                    .setScrollPaneBorder(JBUI.Borders.empty())
                    .setPanelBorder(JBUI.Borders.empty())
                    .setAddAction(__ -> editCertificatePin(null))
                    .setEditAction(__ -> editCertificatePin())
                    .setRemoveAction(__ -> removeCertificatePin())
                    .disableUpDownActions();

            add(decorator.createPanel(), BorderLayout.EAST);
            JScrollPane scrollPane = new JBScrollPane(myList);
            add(scrollPane, BorderLayout.CENTER);

            scrollPane.setPreferredSize(new Dimension(100, 0));
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
