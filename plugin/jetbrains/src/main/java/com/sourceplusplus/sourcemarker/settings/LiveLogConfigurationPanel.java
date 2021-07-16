package com.sourceplusplus.sourcemarker.settings;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import net.miginfocom.swing.*;

public class LiveLogConfigurationPanel extends JPanel {
    public LiveLogConfigurationPanel() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        panel4 = new JPanel();
        label1 = new JLabel();
        textField1 = new JTextField();
        separator2 = new JSeparator();
        panel6 = new JPanel();
        label5 = new JLabel();
        spinner2 = new JSpinner();
        separator1 = new JSeparator();
        panel3 = new JPanel();
        label3 = new JLabel();
        panel1 = new JPanel();
        radioButton2 = new JRadioButton();
        radioButton3 = new JRadioButton();
        radioButton4 = new JRadioButton();
        radioButton5 = new JRadioButton();
        radioButton6 = new JRadioButton();
        radioButton1 = new JRadioButton();
        radioButton7 = new JRadioButton();
        separator3 = new JSeparator();
        panel5 = new JPanel();
        label6 = new JLabel();
        panel2 = new JPanel();
        spinner1 = new JSpinner();
        label7 = new JLabel();
        comboBox2 = new JComboBox<>();

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

            //---- textField1 ----
            textField1.setBackground(new Color(37, 37, 37));
            textField1.setFont(new Font("Roboto Light", Font.PLAIN, 14));
            panel4.add(textField1, "cell 0 1");
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

            //---- spinner2 ----
            spinner2.setBackground(null);
            spinner2.setModel(new SpinnerNumberModel(100, 100, null, 1));
            panel6.add(spinner2, "cell 0 1");
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

                //---- radioButton2 ----
                radioButton2.setText("15 Minutes");
                radioButton2.setSelected(true);
                radioButton2.setBackground(null);
                radioButton2.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(radioButton2, "cell 0 0");

                //---- radioButton3 ----
                radioButton3.setText("30 Minutes");
                radioButton3.setBackground(null);
                radioButton3.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(radioButton3, "cell 1 0");

                //---- radioButton4 ----
                radioButton4.setText("1 Hour");
                radioButton4.setBackground(null);
                radioButton4.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(radioButton4, "cell 2 0");

                //---- radioButton5 ----
                radioButton5.setText("3 Hours");
                radioButton5.setBackground(null);
                radioButton5.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(radioButton5, "cell 3 0");

                //---- radioButton6 ----
                radioButton6.setText("6 Hours");
                radioButton6.setBackground(null);
                radioButton6.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(radioButton6, "cell 4 0");

                //---- radioButton1 ----
                radioButton1.setText("12 Hours");
                radioButton1.setBackground(null);
                radioButton1.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(radioButton1, "cell 5 0");

                //---- radioButton7 ----
                radioButton7.setText("24 Hours");
                radioButton7.setBackground(null);
                radioButton7.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(radioButton7, "cell 6 0");
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
            label6.setText("Rate Limit");
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

                //---- spinner1 ----
                spinner1.setModel(new SpinnerNumberModel(1, 1, null, 1));
                spinner1.setBackground(null);
                panel2.add(spinner1, "cell 0 0");

                //---- label7 ----
                label7.setText("per");
                panel2.add(label7, "cell 1 0");

                //---- comboBox2 ----
                comboBox2.setModel(new DefaultComboBoxModel<>(new String[] {
                    "second"
                }));
                panel2.add(comboBox2, "cell 2 0");
            }
            panel5.add(panel2, "cell 0 1,grow");
        }
        add(panel5, "cell 2 2");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel panel4;
    private JLabel label1;
    private JTextField textField1;
    private JSeparator separator2;
    private JPanel panel6;
    private JLabel label5;
    private JSpinner spinner2;
    private JSeparator separator1;
    private JPanel panel3;
    private JLabel label3;
    private JPanel panel1;
    private JRadioButton radioButton2;
    private JRadioButton radioButton3;
    private JRadioButton radioButton4;
    private JRadioButton radioButton5;
    private JRadioButton radioButton6;
    private JRadioButton radioButton1;
    private JRadioButton radioButton7;
    private JSeparator separator3;
    private JPanel panel5;
    private JLabel label6;
    private JPanel panel2;
    private JSpinner spinner1;
    private JLabel label7;
    private JComboBox<String> comboBox2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
