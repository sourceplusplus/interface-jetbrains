package spp.jetbrains.sourcemarker.element;

import java.awt.*;
import javax.swing.*;

import com.intellij.util.ui.UIUtil;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

public class LiveControlBarRow extends JPanel {

    public LiveControlBarRow() {
        initComponents();
        paintComponent();
    }

    public void setCommandName(String commandName, String input) {
        StringBuilder commandHtml = new StringBuilder();
        String[] inputWords = input.split(" ");
        Color selectColor = Color.decode("#e1483b");
        Color defaultColor = UIUtil.getTextAreaForeground();
        String selectHex = "#"+Integer.toHexString(selectColor.getRGB()).substring(2);
        String defaultHex = "#"+Integer.toHexString(defaultColor.getRGB()).substring(2);

        for (String commandWord : commandName.split(" ")) {

            boolean hasMatch = false;
            for (String inputWord: inputWords) {
                if (commandWord.toLowerCase().startsWith(inputWord)) {
                    commandHtml.append(" ")
                            .append("<span style=\"color: "+selectHex+"\">")
                            .append(commandWord, 0, inputWord.length())
                            .append("</span>")
                            .append("<span style=\"color: "+defaultHex+"\">")
                            .append(commandWord.substring(inputWord.length()))
                            .append("</span>");
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch) {
                commandHtml.append(" ").append("<span style=\"color: "+defaultHex+"\">").append(commandWord).append("</span>");
            }
        }

        commandLabel.setText("<html>" + commandHtml + "</html>");
    }

    public void setCommandIcon(Icon icon) {
        commandIcon.setIcon(icon);
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }

    private void paintComponent() {
        setBackground(UIUtil.getLabelBackground());
        commandLabel.setForeground(UIUtil.getLabelTextForeground());
        descriptionLabel.setForeground(UIUtil.getTextFieldForeground());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        panel1 = new JPanel();
        commandLabel = new JTextPane();
        descriptionLabel = new JTextPane();
        commandIcon = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBackground(new Color(37, 37, 37));
        setMinimumSize(new Dimension(219, 45));
        setMaximumSize(new Dimension(2147483647, 45));
        setPreferredSize(new Dimension(370, 45));
        setLayout(new FormLayout(
            new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                new ColumnSpec(Sizes.dluX(0))
            },
            RowSpec.decodeSpecs("default:grow")));

        //======== panel1 ========
        {
            panel1.setBackground(null);
            panel1.setLayout(new FormLayout(
                ColumnSpec.decodeSpecs("default:grow"),
                new RowSpec[] {
                    FormFactory.DEFAULT_ROWSPEC,
                    FormFactory.DEFAULT_ROWSPEC,
                    FormFactory.LINE_GAP_ROWSPEC,
                    FormFactory.DEFAULT_ROWSPEC
                }));

            //---- commandLabel ----
            commandLabel.setBackground(null);
            commandLabel.setEditable(false);
            commandLabel.setFont(new Font("Roboto", Font.PLAIN, 15));
            commandLabel.setText("<html><span style=\"color: gray; font-size: 15%\">Manual Tracing \u279b Watched Variables \u279b Scope: Local</span></html>");
            commandLabel.setContentType("text/html");
            commandLabel.setMaximumSize(new Dimension(2147483647, 12));
            panel1.add(commandLabel, cc.xy(1, 1));

            //---- descriptionLabel ----
            descriptionLabel.setText("<html><span style=\"color: gray; font-size: 15%\">Manual Tracing \u279b Watched Variables \u279b Scope: Local</span></html>");
            descriptionLabel.setBackground(null);
            descriptionLabel.setFont(new Font("Roboto", Font.PLAIN, 11));
            descriptionLabel.setForeground(Color.gray);
            descriptionLabel.setContentType("text/html");
            descriptionLabel.setEditable(false);
            panel1.add(descriptionLabel, new CellConstraints(1, 2, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(-7, 0, 0, 0)));
        }
        add(panel1, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(3, 10, 0, 0)));
        add(commandIcon, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel panel1;
    private JTextPane commandLabel;
    private JTextPane descriptionLabel;
    private JLabel commandIcon;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
