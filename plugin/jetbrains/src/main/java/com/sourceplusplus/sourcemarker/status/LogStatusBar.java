package com.sourceplusplus.sourcemarker.status;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.UIUtil;
import com.sourceplusplus.marker.source.mark.inlay.InlayMark;
import com.sourceplusplus.protocol.SourceMarkerServices;
import com.sourceplusplus.protocol.artifact.log.Log;
import com.sourceplusplus.protocol.instrument.LiveSourceLocation;
import com.sourceplusplus.protocol.instrument.log.LiveLog;
import com.sourceplusplus.protocol.service.live.LiveInstrumentService;
import com.sourceplusplus.sourcemarker.command.AutocompleteFieldRow;
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys;
import com.sourceplusplus.sourcemarker.psi.LoggerDetector;
import com.sourceplusplus.sourcemarker.settings.LiveLogConfigurationPanel;
import com.sourceplusplus.sourcemarker.status.util.AutocompleteField;
import net.miginfocom.swing.MigLayout;

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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sourceplusplus.sourcemarker.status.util.ViewUtils.addRecursiveMouseListener;

public class LogStatusBar extends JPanel implements VisibleAreaListener {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a")
            .withZone(ZoneId.systemDefault());
    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private final List<AutocompleteFieldRow> scopeVars;
    private final Function<String, List<AutocompleteFieldRow>> lookup;
    private final Pattern VARIABLE_PATTERN;
    private final String placeHolderText;
    private Editor editor;
    private LiveLog liveLog;
    private Instant latestTime;
    private Log latestLog;
    private JWindow popup;
    private LiveLogConfigurationPanel configurationPanel;
    private AtomicBoolean settingFormattedMessage = new AtomicBoolean(false);
    private boolean disposed = false;

    public LogStatusBar(LiveSourceLocation sourceLocation, List<String> scopeVars, InlayMark inlayMark) {
        this(sourceLocation, scopeVars, inlayMark, null, null);
    }

    public LogStatusBar(LiveSourceLocation sourceLocation, List<String> scopeVars, InlayMark inlayMark,
                        LiveLog liveLog, Editor editor) {
        this.sourceLocation = sourceLocation;
        this.scopeVars = scopeVars.stream().map(it -> new AutocompleteFieldRow() {
            public String getText() {
                return "$" + it;
            }

            public String getDescription() {
                return null;
            }

            public Icon getIcon() {
                return IconLoader.getIcon("/nodes/variable.png");
            }
        }).collect(Collectors.toList());
        lookup = text -> scopeVars.stream()
                .filter(v -> {
                    String var = substringAfterLast(" ", text.toLowerCase());
                    return var.startsWith("$") && v.toLowerCase().contains(var.substring(1));
                }).map(it -> new AutocompleteFieldRow() {
                    public String getText() {
                        return "$" + it;
                    }

                    public String getDescription() {
                        return null;
                    }

                    public Icon getIcon() {
                        return IconLoader.getIcon("/nodes/variable.png");
                    }
                })
                .limit(7)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < scopeVars.size(); i++) {
            sb.append("\\$").append(scopeVars.get(i));
            if (i + 1 < scopeVars.size()) {
                sb.append("|");
            }
        }
        sb.append(")(?:\\s|$)");
        VARIABLE_PATTERN = Pattern.compile(sb.toString());

        this.inlayMark = inlayMark;
        this.liveLog = liveLog;
        this.editor = editor;

        if (liveLog != null) {
            placeHolderText = "Waiting for live log data...";
        } else {
            placeHolderText = "Input log message (use $ for variables)";
        }

        initComponents();
        setupComponents();

        if (liveLog != null) {
            liveLogTextField.setEditMode(false);
            removeActiveDecorations();
            addTimeField();
        } else {
            liveLogTextField.setEditMode(true);
        }

        liveLogTextField.addSaveListener(this::saveLiveLog);
    }

    @Override
    public void visibleAreaChanged(VisibleAreaEvent e) {
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
        this.editor = editor;
    }

    public void focus() {
        liveLogTextField.grabFocus();
        liveLogTextField.requestFocusInWindow();
    }

    private void addTimeField() {
        timeLabel.setVisible(true);
        separator1.setVisible(true);
    }

    private void removeActiveDecorations() {
        SwingUtilities.invokeLater(() -> {
//            label7.setIcon(IconLoader.getIcon("/icons/expand.svg"));
//            label8.setIcon(IconLoader.getIcon("/icons/search.svg"));
            closeLabel.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
            configPanel.setBackground(Color.decode("#252525"));

            if (!liveLogTextField.getEditMode()) {
                liveLogTextField.setBorder(new CompoundBorder(
                        new LineBorder(Color.darkGray, 0, true),
                        new EmptyBorder(2, 6, 0, 0)));
                liveLogTextField.setBackground(Color.decode("#2B2B2B"));
                liveLogTextField.setEditable(false);
            }
        });
    }

    private void showEditableMode() {
        liveLogTextField.setBorder(new CompoundBorder(
                new LineBorder(Color.darkGray, 1, true),
                new EmptyBorder(2, 6, 0, 0)));
        liveLogTextField.setBackground(Color.decode("#252525"));
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
                                Pattern.quote("{}"),
                                Matcher.quoteReplacement("$" + var)
                        );
                    }

                    boolean logMessageChanged = !originalMessage.equals(liveLogTextField.getText());
                    if (configurationPanel != null) {
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
                if (liveLogTextField.getEditMode()) return;
                liveLogTextField.reset();
                liveLogTextField.setEditMode(true);

                if (liveLog != null) {
                    String originalMessage = liveLog.getLogFormat();
                    for (String var : liveLog.getLogArguments()) {
                        originalMessage = originalMessage.replaceFirst(
                                Pattern.quote("{}"),
                                Matcher.quoteReplacement("$" + var)
                        );
                    }
                    settingFormattedMessage.set(true);
                    liveLogTextField.setText(originalMessage);
                    settingFormattedMessage.set(false);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (liveLog == null && liveLogTextField.getText().equals("")) {
                    if (popup == null) {
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
                showEditableMode();
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
                closeLabel.setIcon(IconLoader.getIcon("/icons/closeIconHovered.svg"));
            }
        });
        addRecursiveMouseListener(closeLabel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                closeLabel.setIcon(IconLoader.getIcon("/icons/closeIconPressed.svg"));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                closeLabel.setIcon(IconLoader.getIcon("/icons/closeIconHovered.svg"));
            }
        }, () -> {
            removeActiveDecorations();
            return null;
        });

        configPanel.setCursor(Cursor.getDefaultCursor());
        configPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                configPanel.setBackground(Color.decode("#3C3C3C"));
            }
        });

        AtomicLong popupLastOpened = new AtomicLong();
        addRecursiveMouseListener(configPanel, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (System.currentTimeMillis() - popupLastOpened.get() <= 200) {
                    return;
                }

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
        }, () -> {
            removeActiveDecorations();
            return null;
        });

        timeLabel.setCursor(Cursor.getDefaultCursor());

        setCursor(Cursor.getDefaultCursor());
    }

    private void saveLiveLog() {
        if (liveLogTextField.getText().equals("")) {
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
                    LiveLogStatusManager.INSTANCE.removeActiveLiveLog(oldLiveLog);
                } else {
                    it.cause().printStackTrace();
                }
            });
        }

        String logPattern = liveLogTextField.getText();
        ArrayList<String> varMatches = new ArrayList<>();
        Matcher m = VARIABLE_PATTERN.matcher(logPattern);
        while (m.find()) {
            String var = m.group(1);
            logPattern = logPattern.replaceFirst(Pattern.quote(var), "{}");
            varMatches.add(var);
        }
        final String finalLogPattern = logPattern;

        String condition = null;
        long expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * 15);
        int hitRateLimit = 1000;
        int hitLimit = 100;
        if (configurationPanel != null) {
            condition = configurationPanel.getCondition().getExpression();
            expirationDate = Instant.now().toEpochMilli() + (1000L * 60L * configurationPanel.getExpirationInMinutes());
            hitLimit = configurationPanel.getHitLimit();

            configurationPanel.setNewDefaults();
        }

        LiveInstrumentService instrumentService = Objects.requireNonNull(SourceMarkerServices.Instance.INSTANCE.getLiveInstrument());
        LiveLog log = new LiveLog(
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
                hitRateLimit
        );
        instrumentService.addLiveInstrument(log, it -> {
            if (it.succeeded()) {
                inlayMark.putUserData(SourceMarkKeys.INSTANCE.getLOG_ID(), it.result().getId());
                liveLogTextField.setEditMode(false);
                removeActiveDecorations();

                LoggerDetector detector = inlayMark.getUserData(
                        SourceMarkKeys.INSTANCE.getLOGGER_DETECTOR()
                );
                detector.addLiveLog(editor, inlayMark, finalLogPattern, sourceLocation.getLine());
                liveLog = (LiveLog) it.result();
                LiveLogStatusManager.INSTANCE.addActiveLiveLog(liveLog);

                addTimeField();

                //focus back to IDE
                IdeFocusManager.getInstance(editor.getProject())
                        .requestFocusInProject(editor.getContentComponent(), editor.getProject());
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
        inlayMark.dispose(true, false);

        if (liveLog != null) {
            SourceMarkerServices.Instance.INSTANCE.getLiveInstrument().removeLiveInstrument(liveLog.getId(), it -> {
                if (it.succeeded()) {
                    LiveLogStatusManager.INSTANCE.removeActiveLiveLog(liveLog);
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
        timeLabel = new JLabel();
        separator1 = new JSeparator();
        liveLogTextField = new AutocompleteField("$", placeHolderText, scopeVars, lookup, inlayMark.getLineNumber(), false, false);
        closeLabel = new JLabel();

        //======== this ========
        setPreferredSize(new Dimension(500, 40));
        setMinimumSize(new Dimension(500, 40));
        setBorder(new LineBorder(new Color(85, 85, 85)));
        setBackground(new Color(43, 43, 43));
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
            configPanel.setBackground(new Color(37, 37, 37));
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
            configLabel.setIcon(IconLoader.getIcon("/icons/align-left.svg"));
            configPanel.add(configLabel, "cell 0 0");

            //---- configDropdownLabel ----
            configDropdownLabel.setIcon(IconLoader.getIcon("/icons/angle-down.svg"));
            configPanel.add(configDropdownLabel, "cell 1 0");
        }
        add(configPanel, "cell 0 0, grow");

        //---- timeLabel ----
        timeLabel.setIcon(IconLoader.getIcon("/icons/clock.svg"));
        timeLabel.setFont(new Font("Roboto Light", Font.PLAIN, 14));
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
        liveLogTextField.setBackground(new Color(37, 37, 37));
        liveLogTextField.setBorder(new CompoundBorder(
            new LineBorder(Color.darkGray, 1, true),
            new EmptyBorder(2, 6, 0, 0)));
        liveLogTextField.setFont(new Font("Roboto Light", Font.PLAIN, 14));
        liveLogTextField.setMinimumSize(new Dimension(0, 27));
        add(liveLogTextField, "cell 2 0");

        //---- closeLabel ----
        closeLabel.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
        add(closeLabel, "cell 3 0");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel configPanel;
    private JLabel configLabel;
    private JLabel configDropdownLabel;
    private JLabel timeLabel;
    private JSeparator separator1;
    private AutocompleteField liveLogTextField;
    private JLabel closeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
