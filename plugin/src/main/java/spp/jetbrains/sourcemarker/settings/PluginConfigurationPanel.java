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

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static spp.jetbrains.sourcemarker.PluginBundle.message;
import static spp.protocol.SourceServices.Instance.INSTANCE;

public class PluginConfigurationPanel {
    private JPanel myWholePanel;
    private JPanel myGlobalSettingsPanel;
    private JTextField rootSourcePackageTextField;
    private JCheckBox autoResolveEndpointNamesCheckBox;
    private JCheckBox debugConsoleCheckBox;
    private JPanel myServiceSettingsPanel;
    private JTextField serviceHostTextField;
    private JTextField accessTokenTextField;
    private JPanel testPanel;
    private JComboBox serviceComboBox;
    private JCheckBox verifyHostCheckBox;
    private JLabel verifyHostLabel;
    private JCheckBox autoDisplayEndpointQuickStatsCheckBox;
    private JLabel hostLabel;
    private JLabel accessTokenLabel;
    private JLabel certificatePinsLabel;
    private JLabel serviceLabel;
    private JLabel rootSourcePackageLabel;
    private SourceMarkerConfig config;
    private CertificatePinPanel myCertificatePins;

    public PluginConfigurationPanel(SourceMarkerConfig config) {
        this.config = config;
        myServiceSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("service_settings")));
        myGlobalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder(message("plugin_settings")));

        if (INSTANCE.getLiveService() != null) {
            INSTANCE.getLiveService().getServices().onComplete(it -> {
                if (it.succeeded()) {
                    it.result().forEach(service -> serviceComboBox.addItem(service.getName()));

                    if (config.getServiceName() != null) {
                        serviceComboBox.setSelectedItem(config.getServiceName());
                    }
                } else {
                    it.cause().printStackTrace();
                }
            });
        }

        //todo: shouldn't need to manually update locale text
        hostLabel.setText(message("service_host"));
        accessTokenLabel.setText(message("access_token"));
        verifyHostLabel.setText(message("verify_host"));
        certificatePinsLabel.setText(message("certificate_pins"));
        serviceLabel.setText(message("service"));
        rootSourcePackageLabel.setText(message("root_source_package"));
        debugConsoleCheckBox.setText(message("debug_console"));
        autoResolveEndpointNamesCheckBox.setText(message("auto_resolve_endpoint_names"));
        autoDisplayEndpointQuickStatsCheckBox.setText(message("auto_display_endpoint_quick_stats"));
    }

    private void setUIEnabled(boolean enabled) {
        rootSourcePackageTextField.setEnabled(enabled);
        autoResolveEndpointNamesCheckBox.setEnabled(enabled);
        debugConsoleCheckBox.setEnabled(enabled);
        serviceHostTextField.setEnabled(enabled);
        accessTokenTextField.setEnabled(enabled);
        serviceComboBox.setEnabled(enabled);
        verifyHostCheckBox.setEnabled(enabled);
        autoDisplayEndpointQuickStatsCheckBox.setEnabled(enabled);
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
        if (!Objects.equals(debugConsoleCheckBox.isSelected(), config.getPluginConsoleEnabled())) {
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
        if (!Objects.equals(autoDisplayEndpointQuickStatsCheckBox.isSelected(), config.getAutoDisplayEndpointQuickStats())) {
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
                Arrays.stream(rootSourcePackageTextField.getText().split(","))
                        .map(String::trim).collect(Collectors.toList()),
                autoResolveEndpointNamesCheckBox.isSelected(),
                true, debugConsoleCheckBox.isSelected(),
                serviceHostTextField.getText(),
                accessTokenTextField.getText(),
                new ArrayList<>(Collections.list(myCertificatePins.listModel.elements())),
                null,
                verifyHostCheckBox.isSelected(),
                currentService,
                autoDisplayEndpointQuickStatsCheckBox.isSelected(),
                false
        );
    }

    public void applySourceMarkerConfig(SourceMarkerConfig config) {
        this.config = config;
        rootSourcePackageTextField.setText(String.join(",", config.getRootSourcePackages()));
        autoResolveEndpointNamesCheckBox.setSelected(config.getAutoResolveEndpointNames());
        debugConsoleCheckBox.setSelected(config.getPluginConsoleEnabled());
        serviceHostTextField.setText(config.getServiceHost());
        accessTokenTextField.setText(config.getAccessToken());
        verifyHostCheckBox.setSelected(config.getVerifyHost());
        autoDisplayEndpointQuickStatsCheckBox.setSelected(config.getAutoDisplayEndpointQuickStats());

        myCertificatePins = new CertificatePinPanel(!config.getOverride());
        myCertificatePins.listModel.addAll(config.getCertificatePins());
        testPanel.add(myCertificatePins);

        setUIEnabled(!config.getOverride());
    }

    class CertificatePinPanel extends JPanel {
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
