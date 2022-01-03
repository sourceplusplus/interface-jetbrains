package spp.jetbrains.sourcemarker.status;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import io.vertx.core.json.Json;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang.StringUtils;
import spp.jetbrains.marker.source.mark.api.SourceMark;
import spp.jetbrains.marker.source.mark.inlay.InlayMark;
import spp.jetbrains.sourcemarker.PluginIcons;
import spp.jetbrains.sourcemarker.PluginUI;
import spp.jetbrains.sourcemarker.command.AutocompleteFieldRow;
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys;
import spp.jetbrains.sourcemarker.service.breakpoint.BreakpointHitColumnInfo;
import spp.jetbrains.sourcemarker.service.breakpoint.BreakpointHitWindowService;
import spp.jetbrains.sourcemarker.settings.LiveBreakpointConfigurationPanel;
import spp.jetbrains.sourcemarker.status.util.AutocompleteField;
import spp.protocol.SourceMarkerServices;
import spp.protocol.instrument.InstrumentThrottle;
import spp.protocol.instrument.LiveInstrument;
import spp.protocol.instrument.LiveInstrumentEvent;
import spp.protocol.instrument.LiveSourceLocation;
import spp.protocol.instrument.ThrottleStep;
import spp.protocol.instrument.breakpoint.LiveBreakpoint;
import spp.protocol.instrument.breakpoint.event.LiveBreakpointHit;
import spp.protocol.instrument.breakpoint.event.LiveBreakpointRemoved;
import spp.protocol.service.live.LiveInstrumentService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static spp.jetbrains.marker.SourceMarker.conditionParser;
import static spp.jetbrains.sourcemarker.PluginUI.ROBOTO_LIGHT_PLAIN_14;
import static spp.jetbrains.sourcemarker.PluginUI.ROBOTO_LIGHT_PLAIN_15;
import static spp.jetbrains.sourcemarker.status.util.ViewUtils.addRecursiveMouseListener;
import static spp.protocol.instrument.LiveInstrumentEventType.BREAKPOINT_HIT;
import static spp.protocol.instrument.LiveInstrumentEventType.BREAKPOINT_REMOVED;

public class BreakpointStatusBar extends JPanel implements StatusBar, VisibleAreaListener {

    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private EditorImpl editor;
    private JWindow popup;
    private LiveBreakpointConfigurationPanel configurationPanel;
    private boolean disposed = false;
    private final List<AutocompleteFieldRow> scopeVars;
    private final Function<String, List<AutocompleteFieldRow>> lookup;
    private final String placeHolderText = "Breakpoint Condition";
    private LiveBreakpoint liveBreakpoint;
    private LiveBreakpointStatusPanel statusPanel;
    private JPanel wrapper;
    private JPanel panel;
    private JLabel expandLabel;
    private boolean expanded = false;
    private final ListTableModel commandModel = new ListTableModel<>(
            new ColumnInfo[]{
                    new BreakpointHitColumnInfo("Breakpoint Data"),
                    new BreakpointHitColumnInfo("Time")
            },
            new ArrayList<>(), 0, SortOrder.DESCENDING);

    public BreakpointStatusBar(LiveSourceLocation sourceLocation, List<String> scopeVars, InlayMark inlayMark) {
        this.sourceLocation = sourceLocation;
        this.scopeVars = scopeVars.stream().map(it -> new AutocompleteFieldRow() {
            public String getText() {
                return it;
            }

            public String getDescription() {
                return null;
            }

            public Icon getIcon() {
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

                    public Icon getIcon() {
                        return PluginIcons.Nodes.variable;
                    }
                })
                .limit(7)
                .collect(Collectors.toList());

        this.inlayMark = inlayMark;

        initComponents();
        setupComponents();
        paintComponents();
    }

    private void paintComponents() {
        breakpointConditionField.setBackground(UIUtil.getTextFieldBackground());
        breakpointConditionField.setBorder(new CompoundBorder(
                new LineBorder(UIUtil.getBoundsColor(), 1, true),
                new EmptyBorder(2, 6, 0, 0)));
    }

    public void setLiveInstrument(LiveInstrument liveInstrument) {
        this.liveBreakpoint = (LiveBreakpoint) liveInstrument;
        setupAsActive();
    }

    public void setWrapperPanel(JPanel wrapperPanel) {
        this.wrapper = wrapperPanel;
    }

    @Override
    public void visibleAreaChanged(VisibleAreaEvent e) {
        breakpointConditionField.hideAutocompletePopup();
    }

    public void setEditor(Editor editor) {
        this.editor = (EditorImpl) editor;
    }

    public void focus() {
        breakpointConditionField.grabFocus();
        breakpointConditionField.requestFocusInWindow();
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
            if (expandLabel != null) expandLabel.setIcon(PluginIcons.expand);
            closeLabel.setIcon(PluginIcons.close);
            configPanel.setBackground(spp.jetbrains.sourcemarker.PluginUI.getBackgroundDefault());

            if (!breakpointConditionField.getEditMode()) {
                breakpointConditionField.setBorder(new CompoundBorder(
                        new LineBorder(Color.darkGray, 0, true),
                        new EmptyBorder(2, 6, 0, 0)));
                breakpointConditionField.setBackground(spp.jetbrains.sourcemarker.PluginUI.getEditComplete());
                breakpointConditionField.setEditable(false);
            }
        });
    }

    private void setupAsActive() {
        LiveStatusManager.INSTANCE.addStatusBar(inlayMark, event -> {
            if (statusPanel == null) return;
            if (event.getEventType() == BREAKPOINT_HIT) {
                commandModel.insertRow(0, event);
                statusPanel.incrementHits();
            } else if (event.getEventType() == BREAKPOINT_REMOVED) {
                configLabel.setIcon(PluginIcons.eyeSlash);

                LiveBreakpointRemoved removed = Json.decodeValue(event.getData(), LiveBreakpointRemoved.class);
                if (removed.getCause() == null) {
                    statusPanel.setStatus("Complete", Color.decode("#9876AA"));
                } else {
                    commandModel.insertRow(0, event);
                    statusPanel.setStatus("Error", Color.decode("#e1483b"));
                }
            }
        });
        statusPanel = new LiveBreakpointStatusPanel();
        statusPanel.setHitLimit(liveBreakpoint.getHitLimit());

        breakpointConditionField.setEditMode(false);
        removeActiveDecorations();
        configDropdownLabel.setVisible(false);
        SwingUtilities.invokeLater(() -> {
            mainPanel.removeAll();
            mainPanel.setLayout(new BorderLayout());
            statusPanel.setExpires(liveBreakpoint.getExpiresAt());
            mainPanel.add(statusPanel);

            remove(closeLabel);
//                    JLabel searchLabel = new JLabel();
//                    searchLabel.setIcon(PluginIcons.search);
//                    add(searchLabel, "cell 2 0");
            expandLabel = new JLabel();
            expandLabel.setCursor(Cursor.getDefaultCursor());
            expandLabel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    expandLabel.setIcon(PluginIcons.expandHovered);
                }
            });
            addRecursiveMouseListener(expandLabel, new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!expanded) {
                        expanded = true;

                        panel = new JPanel();
                        panel.setLayout(new BorderLayout());
                        JBTable table = new JBTable();
                        JScrollPane scrollPane = new JBScrollPane(table);
                        table.setRowHeight(30);
                        table.setShowColumns(true);
                        table.setModel(commandModel);
                        table.setStriped(true);
                        table.setShowColumns(true);

                        AtomicReference<LiveBreakpointHit> shownBreakpointHit = new AtomicReference<>();
                        table.getSelectionModel().addListSelectionListener(event -> ApplicationManager.getApplication().invokeLater(() -> {
                            if (table.getSelectedRow() > -1) {
                                LiveInstrumentEvent selectedEvent = (LiveInstrumentEvent) commandModel.getItem(
                                        table.convertRowIndexToModel(table.getSelectedRow())
                                );
                                if (selectedEvent.getEventType() == BREAKPOINT_REMOVED) return;
                                LiveBreakpointHit selectedValue = Json.decodeValue(selectedEvent.getData(), LiveBreakpointHit.class);
                                if (selectedValue.equals(shownBreakpointHit.getAndSet(selectedValue))) return;

                                SwingUtilities.invokeLater(() -> {
                                    expanded = false;
                                    wrapper.remove(panel);

                                    BreakpointHitWindowService.Companion.getInstance(inlayMark.getProject())
                                            .clearContent();
                                    BreakpointHitWindowService.Companion.getInstance(inlayMark.getProject())
                                            .showBreakpointHit(shownBreakpointHit.get(), false);
                                });
                            }
                        }));

                        table.setBackground(spp.jetbrains.sourcemarker.PluginUI.getBackgroundDefault());
                        panel.add(scrollPane);
                        panel.setPreferredSize(new Dimension(0, 250));
                        wrapper.add(panel, BorderLayout.NORTH);
                    } else {
                        expanded = false;
                        wrapper.remove(panel);
                    }

                    JViewport viewport = editor.getScrollPane().getViewport();
                    viewport.dispatchEvent(new ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED));
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    expandLabel.setIcon(PluginIcons.expandPressed);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    expandLabel.setIcon(PluginIcons.expandHovered);
                }
            }, () -> {
                removeActiveDecorations();
                return null;
            });
            expandLabel.setIcon(PluginIcons.expand);
            add(expandLabel, "cell 2 0");
            add(closeLabel);
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
        breakpointConditionField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
            }
        });
        breakpointConditionField.addKeyListener(new KeyAdapter() {
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
                } else if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    spinnerTextField.requestFocus();
                }
            }
        });
        breakpointConditionField.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);

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
                    configPanel.setBackground(spp.jetbrains.sourcemarker.PluginUI.getBackgroundFocus());
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
            configurationPanel = new LiveBreakpointConfigurationPanel(breakpointConditionField);
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
        breakpointConditionField.setShowSaveButton(false);

        String condition = null;
        if (!breakpointConditionField.getText().isEmpty()) {
            condition = conditionParser.getCondition(
                    breakpointConditionField.getText(), inlayMark.getPsiElement()
            );
        }

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

            configurationPanel.setNewDefaults();
        }

        HashMap<String, String> meta = new HashMap<>();
        meta.put("original_source_mark", inlayMark.getId());

        LiveInstrumentService instrumentService = Objects.requireNonNull(SourceMarkerServices.Instance.INSTANCE.getLiveInstrument());
        LiveBreakpoint instrument = new LiveBreakpoint(
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
        instrumentService.addLiveInstrument(instrument, it -> {
            if (it.succeeded()) {
                liveBreakpoint = (LiveBreakpoint) it.result();
                inlayMark.putUserData(SourceMarkKeys.INSTANCE.getBREAKPOINT_ID(), liveBreakpoint.getId());
                LiveStatusManager.INSTANCE.addActiveLiveInstrument(liveBreakpoint);
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
        List<SourceMark> groupedMarks = inlayMark.getUserData(SourceMarkKeys.INSTANCE.getGROUPED_MARKS());
        if (groupedMarks != null) groupedMarks.forEach(SourceMark::dispose);

        if (liveBreakpoint != null) {
            SourceMarkerServices.Instance.INSTANCE.getLiveInstrument().removeLiveInstrument(liveBreakpoint.getId(), it -> {
                if (it.succeeded()) {
                    LiveStatusManager.INSTANCE.removeActiveLiveInstrument(liveBreakpoint);
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
        // Generated using JFormDesigner Evaluation license - unknown
        configPanel = new JPanel();
        configLabel = new JLabel();
        configDropdownLabel = new JLabel();
        mainPanel = new JPanel();
        breakpointConditionField = new AutocompleteField(placeHolderText, scopeVars, lookup, inlayMark.getLineNumber(), false, false, Color.decode("#9876AA"));
        label1 = new JLabel();
        hitLimitSpinner = new JBIntSpinner(1, 1, 10_000);
        timeLabel = new JLabel();
        separator1 = new JSeparator();
        closeLabel = new JLabel();

        //======== this ========
        setPreferredSize(new Dimension(500, 40));
        setMinimumSize(new Dimension(500, 40));
        setBorder(PluginUI.PANEL_BORDER);
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "0[fill]" +
            "[grow,fill]" +
            "[fill]",
            // rows
            "0[grow]0"));

        //======== configPanel ========
        {
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
            configLabel.setIcon(PluginIcons.eye);
            configPanel.add(configLabel, "cell 0 0");

            //---- configDropdownLabel ----
            configDropdownLabel.setIcon(PluginIcons.angleDown);
            configPanel.add(configDropdownLabel, "cell 1 0");
        }
        add(configPanel, "cell 0 0, grow");

        //======== mainPanel ========
        {
            mainPanel.setBackground(null);
            mainPanel.setLayout(new MigLayout(
                "novisualpadding,hidemode 3",
                // columns
                "[grow,fill]" +
                "[fill]",
                // rows
                "0[grow]0"));

            //---- breakpointConditionField ----
            breakpointConditionField.setBorder(new CompoundBorder(
                new LineBorder(Color.darkGray, 1, true),
                new EmptyBorder(2, 6, 0, 0)));
            breakpointConditionField.setFont(ROBOTO_LIGHT_PLAIN_14);
            breakpointConditionField.setMinimumSize(new Dimension(0, 27));
            mainPanel.add(breakpointConditionField, "cell 0 0");

            //---- label1 ----
            label1.setText("Hit Limit");
            label1.setForeground(Color.gray);
            label1.setFont(ROBOTO_LIGHT_PLAIN_15);
            mainPanel.add(label1, "cell 1 0");

            //---- hitLimitSpinner ----
            hitLimitSpinner.setBackground(null);
            //hitLimitSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
            mainPanel.add(hitLimitSpinner, "cell 1 0");

            //---- timeLabel ----
            timeLabel.setIcon(PluginIcons.clock);
            timeLabel.setFont(ROBOTO_LIGHT_PLAIN_14);
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
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel configPanel;
    private JLabel configLabel;
    private JLabel configDropdownLabel;
    private JPanel mainPanel;
    private AutocompleteField breakpointConditionField;
    private JLabel label1;
    private JBIntSpinner hitLimitSpinner;
    private JLabel timeLabel;
    private JSeparator separator1;
    private JLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
