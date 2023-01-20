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
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.miginfocom.swing.MigLayout;
import spp.jetbrains.PluginUI;
import spp.jetbrains.UserData;
import spp.jetbrains.icons.PluginIcons;
import spp.jetbrains.marker.SourceMarkerKeys;
import spp.jetbrains.marker.service.ArtifactConditionService;
import spp.jetbrains.marker.source.mark.api.SourceMark;
import spp.jetbrains.marker.source.mark.inlay.InlayMark;
import spp.jetbrains.plugin.LiveStatusBarManager;
import spp.jetbrains.sourcemarker.command.status.ui.config.LiveMeterConfigurationPanel;
import spp.jetbrains.sourcemarker.command.util.AutocompleteField;
import spp.jetbrains.state.LiveStateBar;
import spp.protocol.instrument.LiveInstrument;
import spp.protocol.instrument.LiveSpan;
import spp.protocol.instrument.location.LiveSourceLocation;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static spp.jetbrains.PluginBundle.message;
import static spp.jetbrains.PluginUI.*;
import static spp.jetbrains.utils.ViewUtils.addRecursiveMouseListener;

public class SpanStatusBar extends JPanel implements LiveStateBar, VisibleAreaListener {

    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private EditorImpl editor;
    private JWindow popup;
    private LiveMeterConfigurationPanel configurationPanel;
    private boolean disposed = false;
    private final String placeHolderText = message("operation_name");
    private LiveSpan liveSpan;

    public SpanStatusBar(LiveSourceLocation sourceLocation, InlayMark inlayMark) {
        this.sourceLocation = sourceLocation;
        this.inlayMark = inlayMark;

        initComponents();
        setupComponents();
    }

    public void setLiveInstrument(LiveInstrument liveInstrument) {
        this.liveSpan = (LiveSpan) liveInstrument;
    }

    @Override
    public void visibleAreaChanged(VisibleAreaEvent e) {
        spanOperationNameField.hideAutocompletePopup();
        if (popup != null) {
            popup.dispose();
            popup = null;
        }
    }

    public void setEditor(Editor editor) {
        this.editor = (EditorImpl) editor;
    }

    public void focus() {
        spanOperationNameField.grabFocus();
        spanOperationNameField.requestFocusInWindow();
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
            closeLabel.setIcon(PluginIcons.close);
            configPanel.setBackground(getInputBackgroundColor());

            if (!spanOperationNameField.getEditMode()) {
                spanOperationNameField.setBorder(new CompoundBorder(
                        new LineBorder(JBColor.DARK_GRAY, 0, true),
                        JBUI.Borders.empty(2, 6, 0, 0)));
                spanOperationNameField.setBackground(PluginUI.getEditCompleteColor());
                spanOperationNameField.setEditable(false);
            }
        });
    }

    private void setupComponents() {
        spanOperationNameField.setCanShowSaveButton(false);
        spanOperationNameField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
            }
        });
        spanOperationNameField.addKeyListener(new KeyAdapter() {
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
                    dispose();
                } else if (e.getKeyChar() == KeyEvent.VK_ENTER && spanOperationNameField.getText().length() > 0) {
                    ApplicationManager.getApplication().runWriteAction(() -> saveLiveSpan());
                }
            }
        });
        spanOperationNameField.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);

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
                dispose();
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
        popup = new JWindow(SwingUtilities.getWindowAncestor(SpanStatusBar.this));
        popup.setType(Window.Type.POPUP);
        popup.setAlwaysOnTop(true);

        if (configurationPanel == null) {
            configurationPanel = new LiveMeterConfigurationPanel(spanOperationNameField, inlayMark);
        }

        popup.add(configurationPanel);
        popup.setPreferredSize(new Dimension(SpanStatusBar.this.getWidth(), popup.getPreferredSize().height));
        popup.pack();
        popup.setLocation(configPanel.getLocationOnScreen().x - 1,
                configPanel.getLocationOnScreen().y + SpanStatusBar.this.getHeight() - 2);

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

    private void saveLiveSpan() {
        spanOperationNameField.setShowSaveButton(false);

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

        LiveSpan instrument = new LiveSpan(
                spanOperationNameField.getText(),
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
                liveSpan = (LiveSpan) it.result();
                LiveStatusBarManager.getInstance(inlayMark.getProject()).addActiveLiveInstrument(liveSpan);

                ApplicationManager.getApplication().invokeLater(() -> {
                    inlayMark.dispose(); //dispose this bar
                });
            } else {
                it.cause().printStackTrace();
            }
        });
    }

    private void dispose() {
        if (disposed) return;
        disposed = true;
        editor.getScrollingModel().removeVisibleAreaListener(this);
        if (popup != null) {
            popup.dispose();
            popup = null;
        }
        inlayMark.dispose(true);
        List<SourceMark> groupedMarks = inlayMark.getUserData(SourceMarkerKeys.getGROUPED_MARKS());
        if (groupedMarks != null) groupedMarks.forEach(SourceMark::dispose);

        if (liveSpan != null) {
            UserData.liveInstrumentService(inlayMark.getProject()).removeLiveInstrument(liveSpan.getId()).onComplete(it -> {
                if (it.succeeded()) {
                    LiveStatusBarManager.getInstance(inlayMark.getProject()).removeActiveLiveInstrument(liveSpan);
                } else {
                    it.cause().printStackTrace();
                }
            });
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        setBackground(getBackgroundColor());
        configPanel = new JPanel();
        configLabel = new JLabel();
        configDropdownLabel = new JLabel();
        mainPanel = new JPanel();
        spanOperationNameField = new AutocompleteField(inlayMark.getProject(), placeHolderText, Collections.emptyList(), null, inlayMark.getArtifactQualifiedName(), false);
        timeLabel = new JLabel();
        separator1 = new JSeparator();
        closeLabel = new JLabel();

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
            configLabel.setIcon(PluginIcons.spanConfig);
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
                "[grow,fill]" +
                "[fill]",
                // rows
                "0[grow]0"));

            //---- spanOperationNameField ----
            spanOperationNameField.setBackground(getInputBackgroundColor());
            spanOperationNameField.setBorder(new CompoundBorder(
                    new LineBorder(UIUtil.getBoundsColor(), 1, true),
                    JBUI.Borders.empty(2, 6, 0, 0)));
            spanOperationNameField.setFont(BIG_FONT);
            spanOperationNameField.setMinimumSize(new Dimension(0, 27));
            mainPanel.add(spanOperationNameField, "cell 0 0 2 1");

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
    private JLabel configLabel;
    private JLabel configDropdownLabel;
    private JPanel mainPanel;
    private AutocompleteField spanOperationNameField;
    private JLabel timeLabel;
    private JSeparator separator1;
    private JLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
