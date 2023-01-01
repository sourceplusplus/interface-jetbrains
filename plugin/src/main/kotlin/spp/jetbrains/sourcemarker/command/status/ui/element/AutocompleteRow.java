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

import com.intellij.util.ui.UIUtil;
import net.miginfocom.swing.MigLayout;
import spp.jetbrains.PluginUI;

import javax.swing.*;
import java.awt.*;

import static spp.jetbrains.PluginUI.LABEL_FOREGROUND_COLOR;

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
        commandLabel.setForeground(LABEL_FOREGROUND_COLOR);
        commandLabel.setFont(PluginUI.ROBOTO_LIGHT_BOLD_14);
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
