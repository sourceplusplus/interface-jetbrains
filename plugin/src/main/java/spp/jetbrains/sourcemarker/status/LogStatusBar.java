package spp.jetbrains.sourcemarker.status;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import io.vertx.core.json.Json;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import spp.jetbrains.marker.source.mark.inlay.InlayMark;
import spp.jetbrains.sourcemarker.PluginIcons;
import spp.jetbrains.sourcemarker.PluginUI;
import spp.jetbrains.sourcemarker.VariableParser;
import spp.jetbrains.sourcemarker.command.AutocompleteFieldRow;
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys;
import spp.jetbrains.sourcemarker.service.InstrumentEventListener;
import spp.jetbrains.sourcemarker.service.log.LogHitColumnInfo;
import spp.jetbrains.sourcemarker.settings.LiveLogConfigurationPanel;
import spp.jetbrains.sourcemarker.status.util.AutocompleteField;
import spp.protocol.SourceMarkerServices;
import spp.protocol.artifact.log.Log;
import spp.protocol.instrument.*;
import spp.protocol.instrument.log.LiveLog;
import spp.protocol.instrument.log.event.LiveLogRemoved;
import spp.protocol.service.live.LiveInstrumentService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static spp.jetbrains.marker.SourceMarker.conditionParser;
import static spp.jetbrains.sourcemarker.PluginUI.*;
import static spp.jetbrains.sourcemarker.status.util.ViewUtils.addRecursiveMouseListener;
import static spp.protocol.instrument.LiveInstrumentEventType.LOG_HIT;
import static spp.protocol.instrument.LiveInstrumentEventType.LOG_REMOVED;

public class LogStatusBar extends JPanel implements StatusBar, VisibleAreaListener, InstrumentEventListener {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a")
            .withZone(ZoneId.systemDefault());
    private static final String DOLLAR = "$";
    private static final String EMPTY = "";
    private static final String MESSAGE = "Message";
    private static final String TIME = "Time";
    private static final String SPACE = " ";
    private static final String WAITING_FOR_LIVE_LOG_DATA = "Waiting for live log data...";
    private static final String QUOTE_CURLY_BRACES = Pattern.quote("{}");

    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private final List<AutocompleteFieldRow> scopeVars;
    private final Function<String, List<AutocompleteFieldRow>> lookup;
    private final String placeHolderText;
    private Pattern variablePattern;
    private Pattern templateVariablePattern;
    private EditorImpl editor;
    private LiveLog liveLog;
    private Instant latestTime;
    private Log latestLog;
    private JWindow popup;
    private LiveLogConfigurationPanel configurationPanel;
    private final AtomicBoolean settingFormattedMessage = new AtomicBoolean(false);
    private boolean disposed = false;
    private JLabel expandLabel;
    private boolean expanded;
    private JPanel panel;
    private JPanel wrapper;
    private boolean errored = false;
    private boolean removed = false;
    private final ListTableModel commandModel = new ListTableModel<>(
            new ColumnInfo[]{
                    new LogHitColumnInfo(MESSAGE),
                    new LogHitColumnInfo(TIME)
            },
            new ArrayList<>(), 0, SortOrder.DESCENDING);

    public LogStatusBar(LiveSourceLocation sourceLocation, List<String> scopeVars, InlayMark inlayMark) {
        this.sourceLocation = sourceLocation;
        this.scopeVars = scopeVars.stream().map(it -> new AutocompleteFieldRow() {
            public String getText() {
                return DOLLAR + it;
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
                    String var = substringAfterLast(SPACE, text.toLowerCase());

                    return (var.startsWith(DOLLAR) && !var.substring(1).equals(v)
                                && v.toLowerCase().contains(var.substring(1)))
                            || (var.startsWith("${") && !var.substring(2).equals(v)
                            && v.toLowerCase().contains(var.substring(2)));
                }).map(it -> new AutocompleteFieldRow() {
                    public String getText() {
                        return DOLLAR + it;
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

        if (!scopeVars.isEmpty()) {
            StringBuilder sb = new StringBuilder("(");
            StringBuilder sbt = new StringBuilder("(");
            for (int i = 0; i < scopeVars.size(); i++) {
                sb.append("\\$").append(scopeVars.get(i));
                sbt.append("\\$\\{").append(scopeVars.get(i)).append("\\}");
                if (i + 1 < scopeVars.size()) {
                    sb.append("|");
                    sbt.append("|");
                }
            }
            sb.append(")(?:\\s|$)");
            sbt.append(")(?:|$)");
            variablePattern = Pattern.compile(sb.toString());
            templateVariablePattern = Pattern.compile(sbt.toString());
        }

        this.inlayMark = inlayMark;

        placeHolderText = "Input log message (use $ for variables)";

        initComponents();
        setupComponents();
        showEditableMode();

        liveLogTextField.setEditMode(true);

        liveLogTextField.addSaveListener(this::saveLiveLog);
    }

    public void setLiveInstrument(LiveInstrument liveInstrument) {
        this.liveLog = (LiveLog) liveInstrument;
        liveLogTextField.setEditMode(false);
        liveLogTextField.setPlaceHolderText(WAITING_FOR_LIVE_LOG_DATA);
        wrapper.grabFocus();
        removeActiveDecorations();
        displayTimeField();
        addExpandButton();
        repaint();
        LiveStatusManager.INSTANCE.addStatusBar(inlayMark,this);
    }

    public void setWrapperPanel(JPanel wrapperPanel) {
        this.wrapper = wrapperPanel;
    }

    @Override
    public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
        liveLogTextField.hideAutocompletePopup();
    }

    public void setLatestLog(Instant time, Log latestLog) {
        if (liveLog == null) return;
        this.latestTime = time;
        this.latestLog = latestLog;

        String formattedTime = time.atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
        String formattedMessage = latestLog.getFormattedMessage();
        if (!timeLabel.getText().equals(formattedTime) || !liveLogTextField.getText().equals(formattedMessage)) {
            SwingUtilities.invokeLater(() -> {
                if (liveLogTextField.getEditMode()) {
                    return; //ignore as they're likely updating text
                }

                timeLabel.setText(formattedTime);
                liveLogTextField.setText(formattedMessage);

                liveLogTextField.getStyledDocument().setCharacterAttributes(
                        0, formattedMessage.length(),
                        StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE), true
                );

                int varOffset = 0;
                int minIndex = 0;
                for (String var : latestLog.getArguments()) {
                    int varIndex = latestLog.getContent().indexOf("{}", minIndex);
                    varOffset += varIndex - minIndex;
                    minIndex = varIndex + "{}".length();

                    liveLogTextField.getStyledDocument().setCharacterAttributes(varOffset, var.length(),
                            liveLogTextField.getStyle("numbers"), true);
                    varOffset += var.length();
                }
            });
        }
    }

    public void setEditor(Editor editor) {
        this.editor = (EditorImpl) editor;
    }

    public void focus() {
        liveLogTextField.grabFocus();
        liveLogTextField.requestFocusInWindow();
    }

    private void displayTimeField() {
        timeLabel.setVisible(true);
        separator1.setVisible(true);
    }

    @Override
    public void accept(@NotNull LiveInstrumentEvent event) {
        if (event.getEventType() == LOG_HIT) {
            commandModel.insertRow(0, event);
        } else if (event.getEventType() == LOG_REMOVED) {
            removed = true;

            LiveLogRemoved removed = Json.decodeValue(event.getData(), LiveLogRemoved.class);
            if (removed.getCause() != null) {
                commandModel.insertRow(0, event);

                errored = true;
                liveLogTextField.setText(EMPTY);
                liveLogTextField.setPlaceHolderText(removed.getCause().getMessage());
                liveLogTextField.setPlaceHolderTextColor(SELECT_COLOR_RED);
            }

            liveLogTextField.setEditMode(false);
            configDropdownLabel.setVisible(false);
            removeActiveDecorations();
            repaint();
        }
    }

    private void addExpandButton() {
        if (expandLabel != null) {
            remove(expandLabel);
        }
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

                    table.setBackground(PluginUI.getBackgroundDefaultColor());
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

        remove(closeLabel);
        expandLabel.setIcon(PluginIcons.expand);
        add(expandLabel, "cell 3 0");
        add(closeLabel, "cell 3 0");
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
            if (expandLabel != null) expandLabel.setIcon(PluginIcons.expand);
            closeLabel.setIcon(PluginIcons.close);
            configPanel.setBackground(PluginUI.getBackgroundDefaultColor());

            if (!liveLogTextField.getEditMode()) {
                liveLogTextField.setBorder(new CompoundBorder(
                        new LineBorder(UIUtil.getBoundsColor(), 0, true),
                        new EmptyBorder(2, 6, 0, 0)));
                liveLogTextField.setBackground(PluginUI.getEditCompleteColor());
                liveLogTextField.setEditable(false);
            }
        });
    }

    private void showEditableMode() {
        liveLogTextField.setBorder(new CompoundBorder(
                new LineBorder(UIUtil.getBoundsColor(), 1, true),
                new EmptyBorder(2, 6, 0, 0)));
        liveLogTextField.setBackground(UIUtil.getTextFieldBackground());
        liveLogTextField.setEditable(true);
    }

    private void setupComponents() {
        liveLogTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (settingFormattedMessage.get()) return;
                if (liveLog != null) {
                    String originalMessage = liveLog.getLogFormat();
                    for (String var : liveLog.getLogArguments()) {
                        originalMessage = originalMessage.replaceFirst(
                                QUOTE_CURLY_BRACES,
                                Matcher.quoteReplacement(DOLLAR + var)
                        );
                    }

                    boolean logMessageChanged = !originalMessage.equals(liveLogTextField.getText());
                    if (configurationPanel != null && liveLogTextField.getEditMode()) {
                        liveLogTextField.setShowSaveButton(configurationPanel.isChanged() || logMessageChanged);
                    } else if (liveLogTextField.getEditMode()) {
                        liveLogTextField.setShowSaveButton(logMessageChanged);
                    }
                } else liveLogTextField.setShowSaveButton(!liveLogTextField.getText().isEmpty());
            }
        });
        liveLogTextField.addKeyListener(new KeyAdapter() {
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
                    saveLiveLog();
                }
            }
        });
        liveLogTextField.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
        liveLogTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (errored || liveLogTextField.getEditMode()) return;
                liveLogTextField.setEditMode(true);

                if (liveLog != null) {
                    String originalMessage = liveLog.getLogFormat();
                    for (String var : liveLog.getLogArguments()) {
                        originalMessage = originalMessage.replaceFirst(
                                QUOTE_CURLY_BRACES,
                                Matcher.quoteReplacement(DOLLAR + var)
                        );
                    }
                    settingFormattedMessage.set(true);
                    liveLogTextField.setText(originalMessage);
                    settingFormattedMessage.set(false);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (liveLog == null && liveLogTextField.getText().equals(EMPTY)) {
                    if (popup == null && liveLogTextField.getEditMode()) {
                        dispose();
                    }
                } else if (!liveLogTextField.getEditMode() ||
                        (liveLogTextField.getEditMode() && !liveLogTextField.isShowingSaveButton())) {
                    liveLogTextField.setEditMode(false);
                    removeActiveDecorations();

                    if (latestLog != null) {
                        setLatestLog(latestTime, latestLog);
                    }
                }
            }
        });
        liveLogTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (popup != null) {
                    popup.dispose();
                    popup = null;
                }
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                if (!errored && !removed) showEditableMode();
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                if (!liveLogTextField.getEditMode()) {
                    removeActiveDecorations();
                }
            }
        });

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
                if (!errored && !removed) configPanel.setBackground(PluginUI.getBackgroundFocusColor());
            }
        });

        AtomicLong popupLastOpened = new AtomicLong();
        addRecursiveMouseListener(configPanel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!errored && !removed && System.currentTimeMillis() - popupLastOpened.get() > 200) {
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
        popup = new JWindow(SwingUtilities.getWindowAncestor(LogStatusBar.this));
        popup.setType(Window.Type.POPUP);
        popup.setAlwaysOnTop(true);

        if (configurationPanel == null || !liveLogTextField.isShowingSaveButton()) {
            LiveLogConfigurationPanel previousConfigurationPanel = configurationPanel;
            configurationPanel = new LiveLogConfigurationPanel(liveLogTextField, inlayMark);
            if (previousConfigurationPanel != null) {
                configurationPanel.setCondition(previousConfigurationPanel.getCondition());
                configurationPanel.setExpirationInMinutes(previousConfigurationPanel.getExpirationInMinutes());
                configurationPanel.setHitLimit(previousConfigurationPanel.getHitLimit());
                configurationPanel.setRateLimitCount(previousConfigurationPanel.getRateLimitCount());
                configurationPanel.setRateLimitStep(previousConfigurationPanel.getRateLimitStep());
            } else if (liveLog != null) {
                configurationPanel.setConditionByString(liveLog.getCondition());
                configurationPanel.setHitLimit(liveLog.getHitLimit());
                configurationPanel.setRateLimitCount(liveLog.getThrottle().getLimit());
                configurationPanel.setRateLimitStep(liveLog.getThrottle().getStep().name().toLowerCase());
                //todo: rest
            }
        }

        popup.add(configurationPanel);
        popup.setPreferredSize(new Dimension(LogStatusBar.this.getWidth(), popup.getPreferredSize().height));
        popup.pack();
        popup.setLocation(configPanel.getLocationOnScreen().x - 1,
                configPanel.getLocationOnScreen().y + LogStatusBar.this.getHeight() - 2);

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

    private void saveLiveLog() {
        if (liveLogTextField.getText().equals(EMPTY)) {
            return;
        }
        liveLogTextField.setShowSaveButton(false);

        if (liveLog != null) {
            //editing existing live log; remove old one first
            LiveLog oldLiveLog = liveLog;
            liveLog = null;
            latestTime = null;
            latestLog = null;

            SourceMarkerServices.Instance.INSTANCE.getLiveInstrument().removeLiveInstrument(oldLiveLog.getId(), it -> {
                if (it.succeeded()) {
                    LiveStatusManager.INSTANCE.removeActiveLiveInstrument(oldLiveLog);
                } else {
                    it.cause().printStackTrace();
                }
            });
        }

        ArrayList<String> varMatches = new ArrayList<>();

        String logPattern = VariableParser.extractVariables(variablePattern, templateVariablePattern, varMatches, liveLogTextField.getText());

        final String finalLogPattern = logPattern;

        String condition = null;
        long expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * 15);
        InstrumentThrottle throttle = InstrumentThrottle.Companion.getDEFAULT();
        int hitLimit = 100;
        if (configurationPanel != null) {
            if (configurationPanel.getCondition() != null) {
                condition = conditionParser.getCondition(
                        configurationPanel.getCondition().getExpression(), inlayMark.getPsiElement()
                );
            }

            expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * configurationPanel.getExpirationInMinutes());
            hitLimit = configurationPanel.getHitLimit();
            throttle = new InstrumentThrottle(
                    configurationPanel.getRateLimitCount(),
                    ThrottleStep.valueOf(configurationPanel.getRateLimitStep().toUpperCase())
            );

            configurationPanel.setNewDefaults();
        }

        HashMap<String, String> meta = new HashMap<>();
        meta.put("original_source_mark", inlayMark.getId());

        LiveInstrumentService instrumentService = Objects.requireNonNull(SourceMarkerServices.Instance.INSTANCE.getLiveInstrument());
        LiveLog instrument = new LiveLog(
                finalLogPattern,
                varMatches.stream().map(it -> it.substring(1)).collect(Collectors.toList()),
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

        liveLogTextField.setEditMode(false);
        liveLogTextField.setText(EMPTY);
        liveLogTextField.setPlaceHolderText(WAITING_FOR_LIVE_LOG_DATA);
        removeActiveDecorations();
        displayTimeField();
        wrapper.grabFocus();

        instrumentService.addLiveInstrument(instrument, it -> {
            if (it.succeeded()) {
                liveLog = (LiveLog) it.result();
                inlayMark.putUserData(SourceMarkKeys.INSTANCE.getLOG_ID(), it.result().getId());
                LiveStatusManager.INSTANCE.addActiveLiveInstrument(liveLog);

                inlayMark.getUserData(SourceMarkKeys.INSTANCE.getLOGGER_DETECTOR())
                        .addLiveLog(editor, inlayMark, finalLogPattern, sourceLocation.getLine());
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

        if (liveLog != null) {
            SourceMarkerServices.Instance.INSTANCE.getLiveInstrument().removeLiveInstrument(liveLog.getId(), it -> {
                if (it.succeeded()) {
                    LiveStatusManager.INSTANCE.removeActiveLiveInstrument(liveLog);
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
        configPanel = new JPanel();
        configLabel = new JLabel();
        configDropdownLabel = new JLabel();
        timeLabel = new JLabel();
        separator1 = new JSeparator();
        liveLogTextField = new AutocompleteField(placeHolderText, scopeVars, lookup, inlayMark.getLineNumber(), false, false, COMPLETE_COLOR_PURPLE);
        closeLabel = new JLabel();

        //======== this ========
        setPreferredSize(new Dimension(500, 40));
        setMinimumSize(new Dimension(500, 40));
        setBorder(PluginUI.PANEL_BORDER);
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "0[fill]" +
            "[fill]" +
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
            configLabel.setIcon(PluginIcons.alignLeft);
            configPanel.add(configLabel, "cell 0 0");

            //---- configDropdownLabel ----
            configDropdownLabel.setIcon(PluginIcons.angleDown);
            configPanel.add(configDropdownLabel, "cell 1 0");
        }
        add(configPanel, "cell 0 0, grow");

        //---- timeLabel ----
        timeLabel.setIcon(PluginIcons.clock);
        timeLabel.setFont(ROBOTO_LIGHT_PLAIN_14);
        timeLabel.setIconTextGap(8);
        timeLabel.setVisible(false);
        add(timeLabel, "cell 1 0,gapx null 8");

        //---- separator1 ----
        separator1.setPreferredSize(new Dimension(5, 20));
        separator1.setMinimumSize(new Dimension(5, 20));
        separator1.setOrientation(SwingConstants.VERTICAL);
        separator1.setMaximumSize(new Dimension(5, 20));
        separator1.setVisible(false);
        add(separator1, "cell 1 0");

        //---- liveLogTextField ----
        liveLogTextField.setBackground(UIUtil.getTextFieldBackground());
        liveLogTextField.setBorder(new CompoundBorder(
            new LineBorder(UIUtil.getBoundsColor(), 1, true),
            new EmptyBorder(2, 6, 0, 0)));
        liveLogTextField.setFont(ROBOTO_LIGHT_PLAIN_17);
        liveLogTextField.setMinimumSize(new Dimension(0, 27));
        add(liveLogTextField, "cell 2 0");

        //---- closeLabel ----
        closeLabel.setIcon(PluginIcons.close);
        add(closeLabel, "cell 3 0");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel configPanel;
    private JLabel configLabel;
    private JLabel configDropdownLabel;
    private JLabel timeLabel;
    private JSeparator separator1;
    private AutocompleteField liveLogTextField;
    private JLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
