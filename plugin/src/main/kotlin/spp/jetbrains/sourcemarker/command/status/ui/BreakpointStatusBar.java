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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import spp.jetbrains.PluginUI;
import spp.jetbrains.UserData;
import spp.jetbrains.icons.PluginIcons;
import spp.jetbrains.marker.SourceMarkerKeys;
import spp.jetbrains.marker.service.ArtifactConditionService;
import spp.jetbrains.marker.source.mark.api.SourceMark;
import spp.jetbrains.marker.source.mark.inlay.InlayMark;
import spp.jetbrains.marker.plugin.LiveStatusBarManager;
import spp.jetbrains.sourcemarker.command.status.ui.config.LiveBreakpointConfigurationPanel;
import spp.jetbrains.sourcemarker.command.util.ExpressionUtils;
import spp.jetbrains.sourcemarker.instrument.InstrumentEventWindowService;
import spp.jetbrains.state.LiveStateBar;
import spp.protocol.instrument.LiveBreakpoint;
import spp.protocol.instrument.LiveInstrument;
import spp.protocol.instrument.event.LiveInstrumentRemoved;
import spp.protocol.instrument.location.LiveSourceLocation;
import spp.protocol.instrument.throttle.InstrumentThrottle;
import spp.protocol.instrument.throttle.ThrottleStep;
import spp.protocol.instrument.variable.LiveVariableControl;
import spp.protocol.service.listen.LiveInstrumentListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static spp.jetbrains.PluginBundle.message;
import static spp.jetbrains.PluginUI.*;
import static spp.jetbrains.utils.ViewUtils.addRecursiveMouseListener;

public class BreakpointStatusBar extends JBPanel<BreakpointStatusBar> implements LiveStateBar, LiveInstrumentListener, VisibleAreaListener {

    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private EditorImpl editor;
    private JWindow popup;
    private LiveBreakpointConfigurationPanel configurationPanel;
    private boolean disposed = false;
    private final String placeHolderText = message("breakpoint_condition");
    private LiveBreakpoint liveBreakpoint;

    public BreakpointStatusBar(LiveSourceLocation sourceLocation, InlayMark inlayMark) {
        this.sourceLocation = sourceLocation;
        this.inlayMark = inlayMark;

        initComponents();
        setupComponents();
    }

    @Override
    public void onInstrumentRemovedEvent(@NotNull LiveInstrumentRemoved event) {
        LiveStatusBarManager.getInstance(inlayMark.getProject()).removeActiveLiveInstrument(event.getInstrument());
        this.liveBreakpoint = null;
    }

    public void setLiveInstrument(LiveInstrument liveInstrument) {
        this.liveBreakpoint = (LiveBreakpoint) liveInstrument;
    }

    @Override
    public void visibleAreaChanged(VisibleAreaEvent e) {
        if (popup != null) {
            popup.dispose();
            popup = null;
        }
    }

    public void setEditor(Editor editor) {
        this.editor = (EditorImpl) editor;
    }

    public void focus() {
        IdeFocusManager.getInstance(inlayMark.getProject()).requestFocus(
                breakpointConditionField.getEditorComponent(), true
        );
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
            closeLabel.setIcon(PluginIcons.close);
            configPanel.setBackground(getInputBackgroundColor());
        });
    }

    private void setupComponents() {
        JFormattedTextField spinnerTextField = ((JSpinner.NumberEditor) hitLimitSpinner.getEditor()).getTextField();
        spinnerTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    if (StringUtils.isNumeric(spinnerTextField.getText())) {
                        ApplicationManager.getApplication().runWriteAction(() -> saveLiveBreakpoint());
                    } else {
                        spinnerTextField.setText("1");
                    }
                }
            }
        });
        spinnerTextField.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);

        new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                //ignore
            }
        }.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)),
                hitLimitSpinner.getEditor()
        );

        //go back if shift+tab
        new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                IdeFocusManager.getInstance(inlayMark.getProject())
                        .requestFocus(breakpointConditionField.getEditorComponent(), true);
            }
        }.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)),
                hitLimitSpinner.getEditor()
        );

        ((EditorTextField) breakpointConditionField.getEditorComponent())
                .setNextFocusableComponent(spinnerTextField);
        ((EditorTextField) breakpointConditionField.getEditorComponent())
                .setPlaceholder(placeHolderText);
        ((EditorTextField) breakpointConditionField.getEditorComponent())
                .setShowPlaceholderWhenFocused(true);

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
                Disposer.dispose(BreakpointStatusBar.this);
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
        popup = new JWindow(SwingUtilities.getWindowAncestor(BreakpointStatusBar.this));
        popup.setType(Window.Type.POPUP);
        popup.setAlwaysOnTop(true);

        if (configurationPanel == null) {
            configurationPanel = new LiveBreakpointConfigurationPanel();
        }

        popup.add(configurationPanel);
        popup.setPreferredSize(new Dimension(BreakpointStatusBar.this.getWidth(), popup.getPreferredSize().height));
        popup.pack();
        popup.setLocation(configPanel.getLocationOnScreen().x - 1,
                configPanel.getLocationOnScreen().y + BreakpointStatusBar.this.getHeight() - 2);

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

    private void saveLiveBreakpoint() {
        String condition = null;
        if (!breakpointConditionField.getEditor().getDocument().getText().isEmpty()) {
            condition = ArtifactConditionService.INSTANCE.getCondition(
                    breakpointConditionField.getEditor().getDocument().getText(), inlayMark.getPsiElement()
            );
        }

        LiveVariableControl variableControl = null;
        long expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * 15);
        InstrumentThrottle throttle = InstrumentThrottle.Companion.getDEFAULT();
        JFormattedTextField spinnerTextField = ((JSpinner.NumberEditor) hitLimitSpinner.getEditor()).getTextField();
        int hitLimit = Integer.parseInt(spinnerTextField.getText());
        if (configurationPanel != null) {
            expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * configurationPanel.getExpirationInMinutes());
            throttle = new InstrumentThrottle(
                    configurationPanel.getRateLimitCount(),
                    ThrottleStep.valueOf(configurationPanel.getRateLimitStep().toUpperCase())
            );

            if (configurationPanel.isVariableControlChanged()) {
                variableControl = new LiveVariableControl(
                        configurationPanel.getMaxObjectDepth(),
                        configurationPanel.getMaxObjectSize(),
                        configurationPanel.getMaxCollectionLength(),
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );
            }

            configurationPanel.setNewDefaults();
        }

        HashMap<String, String> meta = new HashMap<>();
        meta.put("original_source_mark", inlayMark.getId());

        LiveBreakpoint instrument = new LiveBreakpoint(
                variableControl,
                sourceLocation,
                condition,
                expirationDate,
                hitLimit,
                null,
                false,
                false,
                false,
                throttle,
                meta
        );
        UserData.liveInstrumentService(inlayMark.getProject()).addLiveBreakpoint(instrument).onComplete(it -> {
            if (it.succeeded()) {
                liveBreakpoint = it.result();

                ApplicationManager.getApplication().invokeLater(() -> {
                    inlayMark.dispose();
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

        if (liveBreakpoint != null) {
            String instrumentId = Objects.requireNonNull(liveBreakpoint.getId());
            if (!InstrumentEventWindowService.getInstance(inlayMark.getProject()).isFinished(instrumentId)) {
                UserData.liveInstrumentService(inlayMark.getProject()).removeLiveInstrument(instrumentId)
                        .onSuccess(it -> LiveStatusBarManager.getInstance(inlayMark.getProject()).removeActiveLiveInstrument(liveBreakpoint))
                        .onFailure(Throwable::printStackTrace);
            }
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        setBackground(getBackgroundColor());
        configPanel = new JBPanel<>();
        configLabel = new JBLabel();
        configDropdownLabel = new JBLabel();
        mainPanel = new JBPanel<>();
        label1 = new JBLabel();
        hitLimitSpinner = new JBIntSpinner(1, 1, 10_000);
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
            configLabel.setIcon(PluginIcons.breakpointConfig);
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

            //---- breakpointConditionField ----
            breakpointConditionField = ExpressionUtils.getExpressionComboBox(
                    inlayMark.getSourceFileMarker().getPsiFile(), inlayMark.getLineNumber(), this,
                    ((JSpinner.NumberEditor) hitLimitSpinner.getEditor()).getTextField(), null
            );
            mainPanel.add(breakpointConditionField.getComboBox(), "cell 0 0");

            //---- label1 ----
            label1.setText(message("hit_limit"));
            label1.setForeground(JBColor.GRAY);
            label1.setFont(SMALLER_FONT);
            mainPanel.add(label1, "cell 1 0");

            //---- hitLimitSpinner ----
            hitLimitSpinner.setBackground(null);
            //hitLimitSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
            mainPanel.add(hitLimitSpinner, "cell 1 0");

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
    private XDebuggerExpressionComboBox breakpointConditionField;
    private JBLabel label1;
    private JBIntSpinner hitLimitSpinner;
    private JBLabel timeLabel;
    private JSeparator separator1;
    private JBLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
