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
package spp.jetbrains.sourcemarker.command.status.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.math.NumberUtils;
import spp.jetbrains.PluginUI;
import spp.jetbrains.UserData;
import spp.jetbrains.icons.PluginIcons;
import spp.jetbrains.marker.SourceMarkerKeys;
import spp.jetbrains.marker.service.ArtifactConditionService;
import spp.jetbrains.marker.source.mark.api.SourceMark;
import spp.jetbrains.marker.source.mark.inlay.InlayMark;
import spp.jetbrains.marker.plugin.LiveStatusBarManager;
import spp.jetbrains.sourcemarker.command.status.ui.config.LiveMeterConfigurationPanel;
import spp.jetbrains.sourcemarker.command.util.AutocompleteField;
import spp.jetbrains.sourcemarker.command.util.AutocompleteFieldRow;
import spp.jetbrains.state.LiveStateBar;
import spp.protocol.instrument.LiveInstrument;
import spp.protocol.instrument.LiveMeter;
import spp.protocol.instrument.location.LiveSourceLocation;
import spp.protocol.instrument.meter.MeterType;
import spp.protocol.instrument.meter.MetricValue;
import spp.protocol.instrument.meter.MetricValueType;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static spp.jetbrains.PluginBundle.message;
import static spp.jetbrains.PluginUI.*;
import static spp.jetbrains.utils.ViewUtils.addRecursiveMouseListener;

public class MeterStatusBar extends JBPanel<MeterStatusBar> implements LiveStateBar, VisibleAreaListener {

    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private EditorImpl editor;
    private JWindow popup;
    private LiveMeterConfigurationPanel configurationPanel;
    private boolean disposed = false;
    private final String meterName = message("meter_name");
    private final String placeHolderText = message("metric_value");
    private LiveMeter liveMeter;
    private final List<AutocompleteFieldRow> scopeVars;
    private final Function<String, List<AutocompleteFieldRow>> lookup;

    public MeterStatusBar(LiveSourceLocation sourceLocation, List<String> scopeVars, InlayMark inlayMark) {
        this.sourceLocation = sourceLocation;
        this.inlayMark = inlayMark;
        this.scopeVars = scopeVars.stream().map(it -> new AutocompleteFieldRow() {
            public String getText() {
                return it;
            }

            public String getDescription() {
                return null;
            }

            public Icon getSelectedIcon() {
                return PluginIcons.Nodes.variable;
            }

            public Icon getUnselectedIcon() {
                return PluginIcons.Nodes.variable;
            }
        }).collect(Collectors.toList());
        lookup = text -> scopeVars.stream()
                .filter(v -> {
                    String var = substringAfterLast(" ", text.toLowerCase());
                    return !var.isEmpty() && !var.equalsIgnoreCase(v) && v.toLowerCase().contains(var);
                }).map(it -> new AutocompleteFieldRow() {
                    public String getText() {
                        return it;
                    }

                    public String getDescription() {
                        return null;
                    }

                    public Icon getSelectedIcon() {
                        return PluginIcons.Nodes.variable;
                    }

                    public Icon getUnselectedIcon() {
                        return PluginIcons.Nodes.variable;
                    }
                })
                .limit(7)
                .collect(Collectors.toList());

        initComponents();
        setupComponents();
    }

    public void setLiveInstrument(LiveInstrument liveInstrument) {
        this.liveMeter = (LiveMeter) liveInstrument;
    }

    @Override
    public void visibleAreaChanged(VisibleAreaEvent e) {
        meterIdField.hideAutocompletePopup();
        if (popup != null) {
            popup.dispose();
            popup = null;
        }
    }

    public void setEditor(Editor editor) {
        this.editor = (EditorImpl) editor;
    }

    public void focus() {
        meterIdField.grabFocus();
        meterIdField.requestFocusInWindow();
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
            closeLabel.setIcon(PluginIcons.close);
            configPanel.setBackground(getInputBackgroundColor());

            if (!meterIdField.getEditMode()) {
                meterIdField.setBorder(new CompoundBorder(
                        new LineBorder(JBColor.DARK_GRAY, 0, true),
                        JBUI.Borders.empty(2, 6, 0, 0)));
                meterIdField.setBackground(PluginUI.getEditCompleteColor());
                meterIdField.setEditable(false);
            }
        });
    }

    private void setupComponents() {
        meterTypeComboBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    ApplicationManager.getApplication().runWriteAction(() -> saveLiveMeter());
                }
            }
        });
        meterTypeComboBox.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);

        meterIdField.setCanShowSaveButton(false);
        meterIdField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_TAB) {
                    e.consume();

                    if (!meterIdField.getText().trim().isEmpty()) {
                        meterConditionField.requestFocus();
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    Disposer.dispose(MeterStatusBar.this);
                } else if (e.getKeyChar() == KeyEvent.VK_ENTER && meterIdField.getText().length() > 0) {
                    meterConditionField.requestFocus();
                }
            }
        });

        meterConditionField.setCanShowSaveButton(false);
        meterConditionField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_TAB) {
                    //ignore tab; handled by auto-complete
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    Disposer.dispose(MeterStatusBar.this);
                } else if (e.getKeyChar() == KeyEvent.VK_ENTER && meterConditionField.getText().length() > 0) {
                    meterTypeComboBox.requestFocus();
                }
            }
        });
        meterIdField.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
        meterConditionField.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                removeActiveDecorations();
            }
        });

        closeLabel.setCursor(Cursor.getDefaultCursor());
        closeLabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closeHovered);
            }
        });
        addRecursiveMouseListener(closeLabel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Disposer.dispose(MeterStatusBar.this);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closePressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                closeLabel.setIcon(PluginIcons.closeHovered);
            }
        }, () -> {
            removeActiveDecorations();
            return null;
        });

        configPanel.setCursor(Cursor.getDefaultCursor());
        configPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (configDropdownLabel.isVisible()) {
                    configPanel.setBackground(getBackgroundFocusColor());
                }
            }
        });

        AtomicLong popupLastOpened = new AtomicLong();
        addRecursiveMouseListener(configPanel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (configDropdownLabel.isVisible() && System.currentTimeMillis() - popupLastOpened.get() > 200) {
                    ApplicationManager.getApplication().runWriteAction(() -> showConfigurationPopup(popupLastOpened));
                }
            }
        }, () -> {
            removeActiveDecorations();
            return null;
        });

        timeLabel.setCursor(Cursor.getDefaultCursor());

        setCursor(Cursor.getDefaultCursor());
    }

    private void showConfigurationPopup(AtomicLong popupLastOpened) {
        popup = new JWindow(SwingUtilities.getWindowAncestor(MeterStatusBar.this));
        popup.setType(Window.Type.POPUP);
        popup.setAlwaysOnTop(true);

        if (configurationPanel == null) {
            configurationPanel = new LiveMeterConfigurationPanel(meterIdField, inlayMark);
        }

        popup.add(configurationPanel);
        popup.setPreferredSize(new Dimension(MeterStatusBar.this.getWidth(), popup.getPreferredSize().height));
        popup.pack();
        popup.setLocation(configPanel.getLocationOnScreen().x - 1,
                configPanel.getLocationOnScreen().y + MeterStatusBar.this.getHeight() - 2);

        popup.setVisible(true);

        popup.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                if (popup != null) {
                    popup.dispose();
                    popup = null;

                    popupLastOpened.set(System.currentTimeMillis());
                }
            }
        });
    }

    private void saveLiveMeter() {
        meterIdField.setShowSaveButton(false);

        String condition = null;
        Long expirationDate = null;
        int hitLimit = -1;
        if (configurationPanel != null) {
            if (configurationPanel.getCondition() != null) {
                condition = ArtifactConditionService.INSTANCE.getCondition(
                        configurationPanel.getCondition().getExpression(), inlayMark.getPsiElement()
                );
            }
            if (configurationPanel.getExpirationInMinutes() != -1) {
                expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * configurationPanel.getExpirationInMinutes());
            }

            configurationPanel.setNewDefaults();
        }

        HashMap<String, String> meta = new HashMap<>();
        meta.put("original_source_mark", inlayMark.getId());

        MetricValue metricValue;
        if (NumberUtils.isCreatable(meterConditionField.getText())) {
            metricValue = new MetricValue(MetricValueType.NUMBER, meterConditionField.getText());
        } else {
            //todo: NUMBER_EXPRESSION
            metricValue = new MetricValue(MetricValueType.VALUE_EXPRESSION, meterConditionField.getText());
        }

        LiveMeter instrument = new LiveMeter(
                MeterType.values()[meterTypeComboBox.getSelectedIndex()],
                metricValue,
//                meterIdField.getText(),
                new ArrayList<>(),
                new ArrayList<>(),
                sourceLocation,
                condition,
                expirationDate,
                hitLimit,
                null,
                false,
                false,
                false,
                null,
                meta
        );
        UserData.liveInstrumentService(inlayMark.getProject()).addLiveInstrument(instrument).onComplete(it -> {
            if (it.succeeded()) {
                liveMeter = (LiveMeter) it.result();
                LiveStatusBarManager.getInstance(inlayMark.getProject()).addActiveLiveInstrument(liveMeter);

                ApplicationManager.getApplication().invokeLater(() -> {
                    inlayMark.dispose(); //dispose this bar

                    //create gutter popup
                    ApplicationManager.getApplication().runReadAction(()
                            -> LiveStatusBarManager.getInstance(inlayMark.getProject()).showMeterStatusIcon(liveMeter, inlayMark.getSourceFileMarker()));
                });
            } else {
                it.cause().printStackTrace();
            }
        });
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        editor.getScrollingModel().removeVisibleAreaListener(this);
        if (popup != null) {
            popup.dispose();
            popup = null;
        }
        inlayMark.dispose();
        List<SourceMark> groupedMarks = inlayMark.getUserData(SourceMarkerKeys.getGROUPED_MARKS());
        if (groupedMarks != null) groupedMarks.forEach(SourceMark::dispose);

        if (liveMeter != null) {
            UserData.liveInstrumentService(inlayMark.getProject()).removeLiveInstrument(liveMeter.getId()).onComplete(it -> {
                if (it.succeeded()) {
                    LiveStatusBarManager.getInstance(inlayMark.getProject()).removeActiveLiveInstrument(liveMeter);
                } else {
                    it.cause().printStackTrace();
                }
            });
        }
    }

    public static String substringAfterLast(String delimiter, String value) {
        int index = value.lastIndexOf(delimiter);
        if (index == -1) return value;
        else return value.substring(index + delimiter.length());
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        setBackground(getBackgroundColor());
        configPanel = new JPanel();
        configLabel = new JBLabel();
        configDropdownLabel = new JBLabel();
        mainPanel = new JPanel();
        meterIdField = new AutocompleteField(inlayMark.getProject(), meterName, Collections.emptyList(), null, inlayMark.getArtifactQualifiedName(), false);
        meterConditionField = new AutocompleteField(inlayMark.getProject(), placeHolderText, scopeVars, lookup, inlayMark.getArtifactQualifiedName(), false);
        label1 = new JBLabel();
        meterTypeComboBox = new ComboBox<>();
        timeLabel = new JBLabel();
        separator1 = new JSeparator();
        closeLabel = new JBLabel();

        //======== this ========
        setPreferredSize(new Dimension(500, 40));
        setMinimumSize(new Dimension(500, 40));
        setBorder(getPanelBorder());
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "0[fill]0" +
            "[grow,fill]" +
            "[fill]",
            // rows
            "0[grow]0"));

        //======== configPanel ========
        {
            configPanel.setBackground(getInputBackgroundColor());
            configPanel.setPreferredSize(null);
            configPanel.setMinimumSize(null);
            configPanel.setMaximumSize(null);
            configPanel.setLayout(new MigLayout(
                "fill,insets 0,hidemode 3",
                // columns
                "5[fill]" +
                "[fill]4",
                // rows
                "[grow]"));

            //---- configLabel ----
            configLabel.setIcon(PluginIcons.meterConfig);
            configPanel.add(configLabel, "cell 0 0");

            //---- configDropdownLabel ----
            configDropdownLabel.setIcon(PluginIcons.angleDown);
            configPanel.add(configDropdownLabel, "cell 1 0");
        }
        add(configPanel, "cell 0 0, grow");

        //======== mainPanel ========
        {
            mainPanel.setBackground(getBackgroundColor());
            mainPanel.setLayout(new MigLayout(
                "novisualpadding,hidemode 3",
                // columns
                "[150,left]" +
                "[grow,fill]" +
                "[fill]",
                // rows
                "0[grow]0"));

            //---- meterNameField ----
            meterIdField.setBackground(getInputBackgroundColor());
            meterIdField.setBorder(new CompoundBorder(
                  new LineBorder(UIUtil.getBoundsColor(), 1, true),
                    JBUI.Borders.empty(2, 6, 0, 0)));
            meterIdField.setFont(BIG_FONT);
            meterIdField.setMinimumSize(new Dimension(0, 27));
            mainPanel.add(meterIdField, "cell 0 0,growx");

            //---- meterConditionField ----
            meterConditionField.setBackground(getInputBackgroundColor());
            meterConditionField.setBorder(new CompoundBorder(
                new LineBorder(UIUtil.getBoundsColor(), 1, true),
                    JBUI.Borders.empty(2, 6, 0, 0)));
            meterConditionField.setFont(BIG_FONT);
            meterConditionField.setMinimumSize(new Dimension(0, 27));
            mainPanel.add(meterConditionField, "cell 1 0");

            //---- label1 ----
            label1.setText(message("type"));
            label1.setForeground(Color.gray);
            label1.setFont(SMALLER_FONT);
            mainPanel.add(label1, "cell 2 0");

            //---- meterTypeComboBox ----
            meterTypeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                message("count"),
                message("gauge"),
                message("histogram")
            }));
            mainPanel.add(meterTypeComboBox, "cell 2 0");

            //---- timeLabel ----
            timeLabel.setIcon(PluginIcons.clock);
            timeLabel.setFont(SMALLEST_FONT);
            timeLabel.setIconTextGap(8);
            timeLabel.setVisible(false);
            mainPanel.add(timeLabel, "cell 1 0,gapx null 8");
        }
        add(mainPanel, "pad 0,cell 1 0,grow,gapx 0 0,gapy 0 0");

        //---- separator1 ----
        separator1.setPreferredSize(new Dimension(5, 20));
        separator1.setMinimumSize(new Dimension(5, 20));
        separator1.setOrientation(SwingConstants.VERTICAL);
        separator1.setMaximumSize(new Dimension(5, 20));
        separator1.setVisible(false);
        add(separator1, "cell 1 0");

        //---- closeLabel ----
        closeLabel.setIcon(PluginIcons.close);
        add(closeLabel, "cell 2 0");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel configPanel;
    private JBLabel configLabel;
    private JBLabel configDropdownLabel;
    private JPanel mainPanel;
    private AutocompleteField meterIdField;
    private AutocompleteField meterConditionField;
    private JBLabel label1;
    private JComboBox<String> meterTypeComboBox;
    private JBLabel timeLabel;
    private JSeparator separator1;
    private JBLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
