package com.sourceplusplus.sourcemarker;

import java.awt.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class Tester extends JPanel {
    public Tester() {
        initComponents();
    }

    public void setCommandName(String commandName) {
        label1.setText(commandName);
    }

    public void setDescription(String description) {
        label2.setText(description);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        label1 = new JLabel();
        label2 = new JLabel();

        //======== this ========
        setBackground(new Color(37, 37, 37));
        setMaximumSize(new Dimension(2147483647, 38));
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "7[grow,fill]",
            // rows
            "2[]2" +
            "[]2"));

        //---- label1 ----
        label1.setText("");
        label1.setForeground(new Color(225, 72, 59));
        label1.setFont(new Font("Roboto Light", Font.BOLD, 14));
        add(label1, "cell 0 0");

        //---- label2 ----
        label2.setText("");
        label2.setForeground(new Color(187, 187, 187, 75));
        add(label2, "cell 0 1");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JLabel label1;
    private JLabel label2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
