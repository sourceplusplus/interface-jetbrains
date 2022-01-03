package spp.jetbrains.sourcemarker.element;

import com.intellij.util.ui.UIUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class AutocompleteRow extends JPanel {

    public AutocompleteRow() {
        initComponents();
        paintComponent();
    }

    public void setCommandName(String commandName) {
        commandLabel.setText(commandName);
    }

    public void setCommandIcon(Icon icon) {
        commandLabel.setIcon(icon);
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }

    private void paintComponent() {
        setBackground(UIUtil.getLabelBackground());
        commandLabel.setForeground(UIUtil.getTextFieldForeground());
        descriptionLabel.setForeground(UIUtil.getTextFieldForeground());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        commandLabel = new JLabel();
        descriptionLabel = new JLabel();

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

        //---- commandLabel ----
        commandLabel.setText("");
        commandLabel.setForeground(new Color(152, 118, 170));
        commandLabel.setFont(new Font("Roboto Light", Font.BOLD, 14));
        add(commandLabel, "cell 0 0");

        //---- descriptionLabel ----
        descriptionLabel.setText("");
        descriptionLabel.setForeground(Color.gray);
        add(descriptionLabel, "cell 0 1");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel commandLabel;
    private JLabel descriptionLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
