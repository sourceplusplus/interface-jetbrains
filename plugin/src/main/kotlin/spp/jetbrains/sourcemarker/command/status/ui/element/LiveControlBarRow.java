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
package spp.jetbrains.sourcemarker.command.status.ui.element;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import org.jetbrains.annotations.NotNull;
import spp.jetbrains.PluginUI;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static spp.jetbrains.PluginUI.*;

public class LiveControlBarRow extends JBPanel<LiveControlBarRow> {

    public LiveControlBarRow() {
        initComponents();
        paintComponent();
    }

    public void setCommandName(String commandName, String input) {
        String selectHex = "#" + Integer.toHexString(PluginUI.getCommandHighlightForeground().getRGB()).substring(2);
        String defaultHex = getCommandTypeColor();

        int minIndex = 0;
        Map<Integer, Integer> matches = new HashMap<>();
        for (String inputWord : input.toLowerCase().split(" ")) {
            int startIndex = commandName.toLowerCase().indexOf(inputWord, minIndex);
            if (startIndex > -1) {
                matches.put(startIndex, inputWord.length());
                minIndex = startIndex + inputWord.length();
            }
        }

        if (!matches.isEmpty()) {
            int diff = 0;
            String updatedCommand = commandName;
            for (Map.Entry<Integer, Integer> entry : matches.entrySet()) {
                String colored = colorMatchingString(updatedCommand, selectHex, entry.getKey() + diff, entry.getValue());
                diff += colored.length() - updatedCommand.length();
                updatedCommand = colored;
            }
            commandName = updatedCommand;
        }
        commandLabel.setText("<html> <span style=\"color: " + defaultHex + "\">" + commandName + "</span></html>");
    }

    @NotNull
    private String colorMatchingString(String commandName, String selectHex, int startIndex, int length) {
        int endIndex = startIndex + length;
        StringBuilder sb = new StringBuilder(commandName.substring(0, startIndex));
        sb.append("<span style=\"color: " + selectHex + "\">");
        sb.append(commandName, startIndex, endIndex);
        sb.append("</span>");
        sb.append(commandName.substring(endIndex));
        commandName = sb.toString();
        return commandName;
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
        panel1 = new JBPanel<>();
        commandLabel = new JTextPane();
        descriptionLabel = new JTextPane();
        commandIcon = new JBLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBackground(PANEL_BACKGROUND_COLOR);
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
            commandLabel.setFont(SMALLER_FONT.deriveFont(Font.BOLD));
            commandLabel.setContentType("text/html");
            commandLabel.setMaximumSize(new Dimension(2147483647, 12));
            commandLabel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            panel1.add(commandLabel, cc.xy(1, 1));

            //---- descriptionLabel ----
            descriptionLabel.setBackground(null);
            descriptionLabel.setFont(SUPER_SMALLEST_FONT);
            descriptionLabel.setContentType("text/html");
            descriptionLabel.setEditable(false);
            descriptionLabel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            panel1.add(descriptionLabel, new CellConstraints(1, 2, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(-7, 0, 0, 0)));
        }
        add(panel1, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(3, 10, 0, 0)));
        add(commandIcon, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel1;
    private JTextPane commandLabel;
    private JTextPane descriptionLabel;
    private JBLabel commandIcon;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
