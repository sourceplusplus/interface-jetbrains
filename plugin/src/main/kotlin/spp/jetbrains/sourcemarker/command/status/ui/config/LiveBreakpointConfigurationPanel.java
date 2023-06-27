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
package spp.jetbrains.sourcemarker.command.status.ui.config;

import javax.swing.border.*;
import com.intellij.ui.components.*;
import com.intellij.ui.components.JBPanel;
import net.miginfocom.swing.MigLayout;
import spp.jetbrains.PluginUI;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static spp.jetbrains.PluginBundle.message;
import static spp.jetbrains.PluginUI.*;

public class LiveBreakpointConfigurationPanel extends JBPanel<LiveBreakpointConfigurationPanel> {

    private int maxObjectDepth = 5;
    private int maxObjectSize = 1024 * 1024;
    private int maxCollectionLength = 100;

    public LiveBreakpointConfigurationPanel() {
        initComponents();

        //todo: set max defaults from probe
        maxObjectSizeComboBox.setSelectedItem("megabytes");
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

    public void setRateLimitCount(int count) {
        rateLimitCountSpinner.setValue(count);
    }

    public String getRateLimitStep() {
        return (String) rateLimitStepCombobox.getSelectedItem();
    }

    public void setRateLimitStep(String step) {
        rateLimitStepCombobox.setSelectedItem(message(step));
    }

    public int getMaxObjectDepth() {
        return (int) maxObjectDepthSpinner.getValue();
    }

    public void setMaxObjectDepth(int maxObjectDepth) {
        this.maxObjectDepth = maxObjectDepth;
        maxObjectDepthSpinner.setValue(maxObjectDepth);
    }

    public int getMaxObjectSize() {
        int size = (int) maxObjectSizeSpinner.getValue();
        int multiplier = 1;
        if (Objects.equals(maxObjectSizeComboBox.getSelectedItem(), "kilobytes")) {
            multiplier = 1024;
        } else if (Objects.equals(maxObjectSizeComboBox.getSelectedItem(), "megabytes")) {
            multiplier = 1024 * 1024;
        }
        return size * multiplier;
    }

    public void setMaxObjectSize(int maxObjectSize) {
        this.maxObjectSize = maxObjectSize;
        maxObjectSizeSpinner.setValue(maxObjectSize);

        if (maxObjectSize < 1024) {
            maxObjectSizeComboBox.setSelectedItem("bytes");
        } else if (maxObjectSize < 1024 * 1024) {
            maxObjectSizeComboBox.setSelectedItem("kilobytes");
        } else {
            maxObjectSizeComboBox.setSelectedItem("megabytes");
        }
    }

    public int getMaxCollectionLength() {
        return (int) maxCollectionLengthSpinner.getValue();
    }

    public void setMaxCollectionLength(int maxCollectionLength) {
        this.maxCollectionLength = maxCollectionLength;
        maxCollectionLengthSpinner.setValue(maxCollectionLength);
    }

    public boolean isVariableControlChanged() {
        return maxObjectDepth != getMaxObjectDepth()
                || maxObjectSize != getMaxObjectSize()
                || maxCollectionLength != getMaxCollectionLength();
    }

    public void setNewDefaults() {
        setExpirationInMinutes(getExpirationInMinutes());
        setRateLimitCount(getRateLimitCount());
        setRateLimitStep(getRateLimitStep());
        setMaxObjectDepth(getMaxObjectDepth());
        setMaxObjectSize(getMaxObjectSize());
        setMaxCollectionLength(getMaxCollectionLength());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Valentino Pecaoco
        panel7 = new JPanel();
        panel8 = new JPanel();
        label4 = new JBLabel();
        label5 = new JBLabel();
        label8 = new JBLabel();
        maxObjectDepthSpinner = new JSpinner();
        separator1 = new JSeparator();
        maxObjectSizeSpinner = new JSpinner();
        maxObjectSizeComboBox = new JComboBox<>();
        separator2 = new JSeparator();
        maxCollectionLengthSpinner = new JSpinner();
        separator4 = new JSeparator();
        panel4 = new JPanel();
        panel3 = new JPanel();
        label3 = new JBLabel();
        panel1 = new JPanel();
        expiration15MinButton = new JBRadioButton();
        expiration30MinButton = new JBRadioButton();
        expiration1HrButton = new JBRadioButton();
        expiration3HrsButton = new JBRadioButton();
        expiration6HrsButton = new JBRadioButton();
        expiration12HrsButton = new JBRadioButton();
        expiration24HrsButton = new JBRadioButton();
        separator3 = new JSeparator();
        panel5 = new JPanel();
        label6 = new JBLabel();
        panel2 = new JPanel();
        rateLimitCountSpinner = new JSpinner();
        label7 = new JBLabel();
        rateLimitStepCombobox = new JComboBox<>();

        //======== this ========
        setBorder(new LineBorder(new Color(0x555555)));
        setBackground(null);
        setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.TitledBorder(new javax.swing.border
        .EmptyBorder(0,0,0,0), "JF\u006frmDes\u0069gner \u0045valua\u0074ion",javax.swing.border.TitledBorder.CENTER,javax
        .swing.border.TitledBorder.BOTTOM,new java.awt.Font("D\u0069alog",java.awt.Font.BOLD,
        12),java.awt.Color.red), getBorder())); addPropertyChangeListener(new java.beans
        .PropertyChangeListener(){@Override public void propertyChange(java.beans.PropertyChangeEvent e){if("\u0062order".equals(e.
        getPropertyName()))throw new RuntimeException();}});
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "[grow,fill]",
            // rows
            "[]" +
            "[]" +
            "[]"));

        //======== panel7 ========
        {
            panel7.setBackground(null);
            panel7.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[grow,fill]",
                // rows
                "[]"));

            //======== panel8 ========
            {
                panel8.setBackground(null);
                panel8.setLayout(new MigLayout(
                    "hidemode 3",
                    // columns
                    "[fill]" +
                    "[5,fill]" +
                    "[fill]0" +
                    "[fill]" +
                    "[5,fill]" +
                    "[fill]",
                    // rows
                    "[]" +
                    "[]"));

                //---- label4 ----
                label4.setText("Max Object Depth");
                panel8.add(label4, "cell 0 0");

                //---- label5 ----
                label5.setText("Max Object Size");
                panel8.add(label5, "cell 2 0 2 1");

                //---- label8 ----
                label8.setText("Max Collection Length");
                panel8.add(label8, "cell 5 0");

                //---- maxObjectDepthSpinner ----
                maxObjectDepthSpinner.setBackground(null);
                maxObjectDepthSpinner.setModel(new SpinnerNumberModel(5, 0, null, 1));
                panel8.add(maxObjectDepthSpinner, "cell 0 1");
                panel8.add(separator1, "cell 1 1");

                //---- maxObjectSizeSpinner ----
                maxObjectSizeSpinner.setBackground(null);
                maxObjectSizeSpinner.setModel(new SpinnerNumberModel(1, 0, null, 1));
                panel8.add(maxObjectSizeSpinner, "cell 2 1");

                //---- maxObjectSizeComboBox ----
                maxObjectSizeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                    "bytes",
                    "kilobytes",
                    "megabytes"
                }));
                panel8.add(maxObjectSizeComboBox, "cell 3 1");
                panel8.add(separator2, "cell 4 1");

                //---- maxCollectionLengthSpinner ----
                maxCollectionLengthSpinner.setBackground(null);
                maxCollectionLengthSpinner.setModel(new SpinnerNumberModel(100, 0, null, 1));
                panel8.add(maxCollectionLengthSpinner, "cell 5 1");
            }
            panel7.add(panel8, "cell 0 0");
        }
        add(panel7, "cell 0 0");
        add(separator4, "cell 0 1");

        //======== panel4 ========
        {
            panel4.setBackground(null);
            panel4.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[grow,fill]" +
                "[fill]" +
                "[100,fill]",
                // rows
                "[]"));

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
            panel4.add(panel3, "cell 0 0");

            //---- separator3 ----
            separator3.setOrientation(SwingConstants.VERTICAL);
            separator3.setPreferredSize(new Dimension(3, 50));
            panel4.add(separator3, "cell 1 0");

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
                    panel2.setBackground(null);
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
                    rateLimitStepCombobox.setModel(new DefaultComboBoxModel<>(new String[] {
                        "second",
                        "minute",
                        "hour"
                    }));
                    panel2.add(rateLimitStepCombobox, "cell 2 0");
                }
                panel5.add(panel2, "cell 0 1,grow");
            }
            panel4.add(panel5, "cell 2 0");
        }
        add(panel4, "cell 0 2");

        //---- expirationButtonGroup ----
        var expirationButtonGroup = new ButtonGroup();
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
    // Generated using JFormDesigner Evaluation license - Valentino Pecaoco
    private JPanel panel7;
    private JPanel panel8;
    private JBLabel label4;
    private JBLabel label5;
    private JBLabel label8;
    private JSpinner maxObjectDepthSpinner;
    private JSeparator separator1;
    private JSpinner maxObjectSizeSpinner;
    private JComboBox<String> maxObjectSizeComboBox;
    private JSeparator separator2;
    private JSpinner maxCollectionLengthSpinner;
    private JSeparator separator4;
    private JPanel panel4;
    private JPanel panel3;
    private JBLabel label3;
    private JPanel panel1;
    private JBRadioButton expiration15MinButton;
    private JBRadioButton expiration30MinButton;
    private JBRadioButton expiration1HrButton;
    private JBRadioButton expiration3HrsButton;
    private JBRadioButton expiration6HrsButton;
    private JBRadioButton expiration12HrsButton;
    private JBRadioButton expiration24HrsButton;
    private JSeparator separator3;
    private JPanel panel5;
    private JBLabel label6;
    private JPanel panel2;
    private JSpinner rateLimitCountSpinner;
    private JBLabel label7;
    private JComboBox<String> rateLimitStepCombobox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
