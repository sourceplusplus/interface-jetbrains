package com.sourceplusplus.sourcemarker.settings;

import com.sourceplusplus.sourcemarker.status.util.AutocompleteField;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Objects;

public class LiveMeterConfigurationPanel extends JPanel {

    private int hitLimit = 100;
    private int expirationInMinutes = 15;

    public LiveMeterConfigurationPanel(AutocompleteField autocompleteField) {
        initComponents();

        expiration15MinButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration30MinButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration1HrButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration3HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration6HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration12HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration24HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));

        hitLimitSpinner.addChangeListener(changeEvent -> autocompleteField.setShowSaveButton(isChanged()));
    }

    public void setHitLimit(int hitLimit) {
        this.hitLimit = hitLimit;
        hitLimitSpinner.setValue(hitLimit);
    }

    public int getHitLimit() {
        return (int) hitLimitSpinner.getValue();
    }

    public int getExpirationInMinutes() {
        if (expiration15MinButton.isSelected()) {
            return 15;
        } else if (expiration30MinButton.isSelected()) {
            return 30;
        } else if (expiration1HrButton.isSelected()) {
            return 60;
        } else if (expiration3HrsButton.isSelected()) {
            return 60 * 3;
        } else if (expiration6HrsButton.isSelected()) {
            return 60 * 6;
        } else if (expiration12HrsButton.isSelected()) {
            return 60 * 12;
        } else if (expiration24HrsButton.isSelected()) {
            return 60 * 24;
        } else {
            throw new IllegalStateException();
        }
    }

    public void setExpirationInMinutes(int value) {
        this.expirationInMinutes = value;

        if (value == 15) {
            expiration15MinButton.setSelected(true);
        } else if (value == 30) {
            expiration30MinButton.setSelected(true);
        } else if (value == 60) {
            expiration1HrButton.setSelected(true);
        } else if (value == 60 * 3) {
            expiration3HrsButton.setSelected(true);
        } else if (value == 60 * 6) {
            expiration6HrsButton.setSelected(true);
        } else if (value == 60 * 12) {
            expiration12HrsButton.setSelected(true);
        } else if (value == 60 * 24) {
            expiration24HrsButton.setSelected(true);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getRateLimitCount() {
        return (int) rateLimitCountSpinner.getValue();
    }


    public String getRateLimitStep() {
        return (String) rateLimitStepCombobox.getSelectedItem();
    }

    public boolean isChanged() {
        return hitLimit != getHitLimit() || expirationInMinutes != getExpirationInMinutes();
    }

    public void setNewDefaults() {
        setHitLimit(getHitLimit());
        setExpirationInMinutes(getExpirationInMinutes());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        panel4 = new JPanel();
        label1 = new JLabel();
        conditionPanel = new JPanel();
        separator2 = new JSeparator();
        panel6 = new JPanel();
        label5 = new JLabel();
        hitLimitSpinner = new JSpinner();
        separator1 = new JSeparator();
        panel3 = new JPanel();
        label3 = new JLabel();
        panel1 = new JPanel();
        expiration15MinButton = new JRadioButton();
        expiration30MinButton = new JRadioButton();
        expiration1HrButton = new JRadioButton();
        expiration3HrsButton = new JRadioButton();
        expiration6HrsButton = new JRadioButton();
        expiration12HrsButton = new JRadioButton();
        expiration24HrsButton = new JRadioButton();
        separator3 = new JSeparator();
        panel5 = new JPanel();
        label6 = new JLabel();
        panel2 = new JPanel();
        rateLimitCountSpinner = new JSpinner();
        label7 = new JLabel();
        rateLimitStepCombobox = new JComboBox<>();

        //======== this ========
        setBackground(new Color(43, 43, 43));
        setBorder(new LineBorder(new Color(85, 85, 85)));
        setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[grow,fill]" +
                        "[fill]" +
                        "[100,fill]",
                // rows
                "[]" +
                        "[]" +
                        "[]"));

        //======== panel4 ========
        {
            panel4.setBackground(null);
            panel4.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "[100,grow,fill]",
                    // rows
                    "[]" +
                            "[]"));

            //---- label1 ----
            label1.setText("Condtion");
            label1.setFont(new Font("Roboto Light", Font.PLAIN, 15));
            panel4.add(label1, "cell 0 0");

            //======== conditionPanel ========
            {
                conditionPanel.setMinimumSize(new Dimension(0, 27));
                conditionPanel.setLayout(new BorderLayout());
            }
            panel4.add(conditionPanel, "cell 0 1");
        }
        add(panel4, "cell 0 0");

        //---- separator2 ----
        separator2.setOrientation(SwingConstants.VERTICAL);
        separator2.setPreferredSize(new Dimension(3, 50));
        add(separator2, "cell 1 0");

        //======== panel6 ========
        {
            panel6.setBackground(null);
            panel6.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "[grow,fill]",
                    // rows
                    "[]" +
                            "[grow]"));

            //---- label5 ----
            label5.setText("Hit Limit");
            label5.setFont(new Font("Roboto Light", Font.PLAIN, 15));
            panel6.add(label5, "cell 0 0");

            //---- hitLimitSpinner ----
            hitLimitSpinner.setBackground(null);
            hitLimitSpinner.setModel(new SpinnerNumberModel(100, 1, null, 1));
            panel6.add(hitLimitSpinner, "cell 0 1");
        }
        add(panel6, "cell 2 0");
        add(separator1, "cell 0 1 3 1");

        //======== panel3 ========
        {
            panel3.setBackground(null);
            panel3.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "[150,fill]" +
                            "[fill]" +
                            "[150,fill]",
                    // rows
                    "[]" +
                            "[]"));

            //---- label3 ----
            label3.setText("Expiration Date");
            label3.setFont(new Font("Roboto Light", Font.PLAIN, 15));
            panel3.add(label3, "cell 0 0");

            //======== panel1 ========
            {
                panel1.setBackground(null);
                panel1.setLayout(new MigLayout(
                        "hidemode 3",
                        // columns
                        "[fill]" +
                                "[fill]" +
                                "[fill]" +
                                "[fill]" +
                                "[fill]" +
                                "[fill]" +
                                "[fill]" +
                                "[fill]",
                        // rows
                        "[]"));

                //---- expiration15MinButton ----
                expiration15MinButton.setText("15 Minutes");
                expiration15MinButton.setSelected(true);
                expiration15MinButton.setBackground(null);
                expiration15MinButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration15MinButton, "cell 0 0");

                //---- expiration30MinButton ----
                expiration30MinButton.setText("30 Minutes");
                expiration30MinButton.setBackground(null);
                expiration30MinButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration30MinButton, "cell 1 0");

                //---- expiration1HrButton ----
                expiration1HrButton.setText("1 Hour");
                expiration1HrButton.setBackground(null);
                expiration1HrButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration1HrButton, "cell 2 0");

                //---- expiration3HrsButton ----
                expiration3HrsButton.setText("3 Hours");
                expiration3HrsButton.setBackground(null);
                expiration3HrsButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration3HrsButton, "cell 3 0");

                //---- expiration6HrsButton ----
                expiration6HrsButton.setText("6 Hours");
                expiration6HrsButton.setBackground(null);
                expiration6HrsButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration6HrsButton, "cell 4 0");

                //---- expiration12HrsButton ----
                expiration12HrsButton.setText("12 Hours");
                expiration12HrsButton.setBackground(null);
                expiration12HrsButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration12HrsButton, "cell 5 0");

                //---- expiration24HrsButton ----
                expiration24HrsButton.setText("24 Hours");
                expiration24HrsButton.setBackground(null);
                expiration24HrsButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration24HrsButton, "cell 6 0");
            }
            panel3.add(panel1, "cell 0 1 3 1");
        }
        add(panel3, "cell 0 2");

        //---- separator3 ----
        separator3.setOrientation(SwingConstants.VERTICAL);
        separator3.setPreferredSize(new Dimension(3, 50));
        add(separator3, "cell 1 2");

        //======== panel5 ========
        {
            panel5.setBackground(null);
            panel5.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "[fill]",
                    // rows
                    "[]" +
                            "[grow]"));

            //---- label6 ----
            label6.setText("Hit Throttle");
            label6.setFont(new Font("Roboto Light", Font.PLAIN, 15));
            panel5.add(label6, "cell 0 0");

            //======== panel2 ========
            {
                panel2.setBackground(new Color(43, 43, 43));
                panel2.setLayout(new MigLayout(
                        "hidemode 3",
                        // columns
                        "0[fill]" +
                                "[fill]" +
                                "[fill]",
                        // rows
                        "[]"));

                //---- rateLimitCountSpinner ----
                rateLimitCountSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
                rateLimitCountSpinner.setBackground(null);
                panel2.add(rateLimitCountSpinner, "cell 0 0");

                //---- label7 ----
                label7.setText("per");
                panel2.add(label7, "cell 1 0");

                //---- rateLimitStepCombobox ----
                rateLimitStepCombobox.setModel(new DefaultComboBoxModel<>(new String[]{
                        "second",
                        "minute",
                        "hour"
                }));
                panel2.add(rateLimitStepCombobox, "cell 2 0");
            }
            panel5.add(panel2, "cell 0 1,grow");
        }
        add(panel5, "cell 2 2");

        //---- expirationButtonGroup ----
        ButtonGroup expirationButtonGroup = new ButtonGroup();
        expirationButtonGroup.add(expiration15MinButton);
        expirationButtonGroup.add(expiration30MinButton);
        expirationButtonGroup.add(expiration1HrButton);
        expirationButtonGroup.add(expiration3HrsButton);
        expirationButtonGroup.add(expiration6HrsButton);
        expirationButtonGroup.add(expiration12HrsButton);
        expirationButtonGroup.add(expiration24HrsButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel panel4;
    private JLabel label1;
    private JPanel conditionPanel;
    private JSeparator separator2;
    private JPanel panel6;
    private JLabel label5;
    private JSpinner hitLimitSpinner;
    private JSeparator separator1;
    private JPanel panel3;
    private JLabel label3;
    private JPanel panel1;
    private JRadioButton expiration15MinButton;
    private JRadioButton expiration30MinButton;
    private JRadioButton expiration1HrButton;
    private JRadioButton expiration3HrsButton;
    private JRadioButton expiration6HrsButton;
    private JRadioButton expiration12HrsButton;
    private JRadioButton expiration24HrsButton;
    private JSeparator separator3;
    private JPanel panel5;
    private JLabel label6;
    private JPanel panel2;
    private JSpinner rateLimitCountSpinner;
    private JLabel label7;
    private JComboBox<String> rateLimitStepCombobox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

}
