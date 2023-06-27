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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.*;
import com.intellij.ui.components.JBPanel;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import spp.jetbrains.marker.service.ArtifactConditionService;
import spp.jetbrains.marker.source.mark.inlay.InlayMark;
import spp.jetbrains.sourcemarker.command.util.AutocompleteField;
import spp.jetbrains.sourcemarker.command.util.ExpressionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static spp.jetbrains.PluginBundle.message;
import static spp.jetbrains.PluginUI.*;

public class LiveMeterConfigurationPanel extends JBPanel<LiveMeterConfigurationPanel> {

    private final XDebuggerExpressionComboBox comboBox;
    private XExpression condition;
    private int expirationInMinutes = 15;

    public LiveMeterConfigurationPanel(AutocompleteField<?> autocompleteField, InlayMark inlayMark) {
        comboBox = ExpressionUtils.getExpressionComboBox(
                inlayMark.getSourceFileMarker().getPsiFile(), inlayMark.getLineNumber(),
                null, null, null
        );

        initComponents();

        EditorTextField editorTextField = (EditorTextField) comboBox.getEditorComponent();
        editorTextField.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                autocompleteField.setShowSaveButton(isChanged());
            }
        });
        expirationNeverButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration15MinButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration30MinButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration1HrButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration3HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration6HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration12HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));
        expiration24HrsButton.addActionListener(actionEvent -> autocompleteField.setShowSaveButton(isChanged()));

        conditionPanel.add(comboBox.getComponent());
    }

    public void setConditionByString(String condition) {
        if (condition == null) {
            setCondition(null);
        } else {
            setCondition(XExpressionImpl.fromText(ArtifactConditionService.fromLiveConditional(condition)));
        }
    }

    public void setCondition(XExpression condition) {
        this.condition = condition;
        ApplicationManager.getApplication().runWriteAction(() -> comboBox.setExpression(condition));
    }

    public XExpression getCondition() {
        return comboBox.getExpression();
    }

    public int getExpirationInMinutes() {
        if (expirationNeverButton.isSelected()) {
            return -1;
        } else if (expiration15MinButton.isSelected()) {
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

        if (value == -1) {
            expirationNeverButton.setSelected(true);
        } else if (value == 15) {
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

    public boolean isChanged() {
        return ((condition == null && !getCondition().getExpression().isEmpty()) || (condition != null && !Objects.equals(condition.getExpression(), getCondition().getExpression())))
                || expirationInMinutes != getExpirationInMinutes();
    }

    public void setNewDefaults() {
        setCondition(getCondition());
        setExpirationInMinutes(getExpirationInMinutes());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Valentino Pecaoco
        panel4 = new JPanel();
        label1 = new JBLabel();
        conditionPanel = new JPanel();
        separator1 = new JSeparator();
        panel3 = new JPanel();
        label3 = new JLabel();
        panel1 = new JPanel();
        expirationNeverButton = new JBRadioButton();
        expiration15MinButton = new JBRadioButton();
        expiration30MinButton = new JBRadioButton();
        expiration1HrButton = new JBRadioButton();
        expiration3HrsButton = new JBRadioButton();
        expiration6HrsButton = new JBRadioButton();
        expiration12HrsButton = new JBRadioButton();
        expiration24HrsButton = new JBRadioButton();

        //======== this ========
        setBorder(new LineBorder(new Color(0x555555)));
        setBorder ( new javax . swing. border .CompoundBorder ( new javax . swing. border .TitledBorder ( new javax . swing. border .EmptyBorder ( 0
        , 0 ,0 , 0) ,  "JF\u006frm\u0044es\u0069gn\u0065r \u0045va\u006cua\u0074io\u006e" , javax. swing .border . TitledBorder. CENTER ,javax . swing. border .TitledBorder . BOTTOM
        , new java. awt .Font ( "D\u0069al\u006fg", java .awt . Font. BOLD ,12 ) ,java . awt. Color .red ) ,
         getBorder () ) );  addPropertyChangeListener( new java. beans .PropertyChangeListener ( ){ @Override public void propertyChange (java . beans. PropertyChangeEvent e
        ) { if( "\u0062or\u0064er" .equals ( e. getPropertyName () ) )throw new RuntimeException( ) ;} } );
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "[grow,fill]",
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
        add(separator1, "cell 0 1");

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
                    "[fill]" +
                    "[fill]",
                    // rows
                    "[]" +
                    "[]"));

                //---- expirationNeverButton ----
                expirationNeverButton.setText("Never");
                expirationNeverButton.setBackground(null);
                expirationNeverButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                expirationNeverButton.setSelected(true);
                panel1.add(expirationNeverButton, "cell 0 0,alignx center,growx 0");

                //---- expiration15MinButton ----
                expiration15MinButton.setText("15 Minutes");
                expiration15MinButton.setBackground(null);
                expiration15MinButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration15MinButton, "cell 1 0,alignx center,growx 0");

                //---- expiration30MinButton ----
                expiration30MinButton.setText("30 Minutes");
                expiration30MinButton.setBackground(null);
                expiration30MinButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration30MinButton, "cell 2 0,alignx center,growx 0");

                //---- expiration1HrButton ----
                expiration1HrButton.setText("1 Hour");
                expiration1HrButton.setBackground(null);
                expiration1HrButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration1HrButton, "cell 3 0,alignx center,growx 0");

                //---- expiration3HrsButton ----
                expiration3HrsButton.setText("3 Hours");
                expiration3HrsButton.setBackground(null);
                expiration3HrsButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration3HrsButton, "cell 4 0,alignx center,growx 0");

                //---- expiration6HrsButton ----
                expiration6HrsButton.setText("6 Hours");
                expiration6HrsButton.setBackground(null);
                expiration6HrsButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration6HrsButton, "cell 5 0,alignx center,growx 0");

                //---- expiration12HrsButton ----
                expiration12HrsButton.setText("12 Hours");
                expiration12HrsButton.setBackground(null);
                expiration12HrsButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration12HrsButton, "cell 6 0,alignx center,growx 0");

                //---- expiration24HrsButton ----
                expiration24HrsButton.setText("24 Hours");
                expiration24HrsButton.setBackground(null);
                expiration24HrsButton.setFont(new Font("Roboto Light", Font.PLAIN, 15));
                panel1.add(expiration24HrsButton, "cell 7 0,alignx center,growx 0");
            }
            panel3.add(panel1, "cell 0 1 3 1");
        }
        add(panel3, "cell 0 2");

        //---- expirationButtonGroup ----
        var expirationButtonGroup = new ButtonGroup();
        expirationButtonGroup.add(expirationNeverButton);
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
    private JPanel panel4;
    private JBLabel label1;
    private JPanel conditionPanel;
    private JSeparator separator1;
    private JPanel panel3;
    private JLabel label3;
    private JPanel panel1;
    private JBRadioButton expirationNeverButton;
    private JBRadioButton expiration15MinButton;
    private JBRadioButton expiration30MinButton;
    private JBRadioButton expiration1HrButton;
    private JBRadioButton expiration3HrsButton;
    private JBRadioButton expiration6HrsButton;
    private JBRadioButton expiration12HrsButton;
    private JBRadioButton expiration24HrsButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
