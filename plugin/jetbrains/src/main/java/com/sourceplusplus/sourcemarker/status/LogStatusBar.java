package com.sourceplusplus.sourcemarker.status;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.IdeFocusManager;
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
import com.sourceplusplus.sourcemarker.status.util.AutocompleteField;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sourceplusplus.sourcemarker.status.util.ViewUtils.addRecursiveMouseListener;

public class LogStatusBar extends JPanel {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a")
            .withZone(ZoneId.systemDefault());
    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private final List<AutocompleteFieldRow> scopeVars;
    private final Function<String, List<AutocompleteFieldRow>> lookup;
    private final Pattern VARIABLE_PATTERN;
    private Editor editor;
    private LiveLog liveLog;

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
                    String var = substringAfterLast(" ", text);
                    return var.startsWith("$") && v.toLowerCase().contains(var.substring(1))
                            && !v.toLowerCase().equals(var.substring(1));
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

        initComponents();
        setupComponents();

        if (liveLog != null) {
            addTimeField();
            removeActiveDecorations();
            ((AutocompleteField) textField1).setEditMode(false);
        } else {
            //edit mode
            ((AutocompleteField) textField1).setEditMode(true);
        }
    }

    public void setLatestLog(Instant time, Log latestLog) {
        if (((AutocompleteField) textField1).getEditMode()) {
            return; //ignore as they're likely updating text
        }

        String formattedTime = time.atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
        String formattedMessage = latestLog.getFormattedMessage();
        if (!label4.getText().equals(formattedTime) || !textField1.getText().equals(formattedMessage)) {
            SwingUtilities.invokeLater(() -> {
                label4.setText(formattedTime);
                textField1.setText(formattedMessage);

                textField1.getStyledDocument().setCharacterAttributes(
                        0, formattedMessage.length(),
                        StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE), true
                );

                int varOffset = 0;
                int minIndex = 0;
                for (String var : latestLog.getArguments()) {
                    int varIndex = latestLog.getContent().indexOf("{}", minIndex);
                    varOffset += varIndex - minIndex;
                    minIndex = varIndex + "{}".length();

                    textField1.getStyledDocument().setCharacterAttributes(varOffset, var.length(),
                            textField1.getStyle("numbers"), true);
                    varOffset += var.length();
                }
            });
        }
    }

    public void setEditor(Editor editor) {
        this.editor = editor;
    }

    public void focus() {
        textField1.grabFocus();
        textField1.requestFocusInWindow();
    }

    private void addTimeField() {
        label4.setVisible(true);
        separator1.setVisible(true);
    }

    private void removeActiveDecorations() {
//        label7.setIcon(IconLoader.getIcon("/icons/expand.svg"));
//        label8.setIcon(IconLoader.getIcon("/icons/search.svg"));
        label2.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
        panel1.setBackground(Color.decode("#252525"));

        if (!((AutocompleteField) textField1).getEditMode()) {
            textField1.setBorder(new CompoundBorder(
                    new LineBorder(Color.darkGray, 0, true),
                    new EmptyBorder(2, 6, 0, 0)));
            textField1.setBackground(Color.decode("#2B2B2B"));
            textField1.setEditable(false);
        }
    }

    private void setupComponents() {
        textField1.addKeyListener(new KeyAdapter() {
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
                    if (textField1.getText().equals("")) {
                        return;
                    }

                    String logPattern = textField1.getText();
                    ArrayList<String> varMatches = new ArrayList<>();
                    Matcher m = VARIABLE_PATTERN.matcher(logPattern);
                    while (m.find()) {
                        String var = m.group(1);
                        logPattern = logPattern.replaceFirst(Pattern.quote(var), "{}");
                        varMatches.add(var);
                    }
                    final String finalLogPattern = logPattern;

                    LiveInstrumentService instrumentService = Objects.requireNonNull(SourceMarkerServices.Instance.INSTANCE.getLiveInstrument());
                    LiveLog log = new LiveLog(
                            finalLogPattern,
                            varMatches.stream().map(it -> it.substring(1)).collect(Collectors.toList()),
                            sourceLocation,
                            null,
                            null,
                            Integer.MAX_VALUE,
                            null,
                            false,
                            false,
                            false,
                            1000
                    );
                    instrumentService.addLiveInstrument(log, it -> {
                        if (it.succeeded()) {
                            inlayMark.putUserData(SourceMarkKeys.INSTANCE.getLOG_ID(), it.result().getId());
                            ((AutocompleteField) textField1).setEditMode(false);
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
            }
        });
        textField1.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
        textField1.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                ((AutocompleteField) textField1).setEditMode(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (liveLog == null && textField1.getText().equals("")) {
                    dispose();
                } else if (liveLog != null) {
                    ((AutocompleteField) textField1).setEditMode(false);
                    removeActiveDecorations();
                }
            }
        });
        textField1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                textField1.setBorder(new CompoundBorder(
                        new LineBorder(Color.darkGray, 1, true),
                        new EmptyBorder(2, 6, 0, 0)));
                textField1.setBackground(Color.decode("#252525"));
                textField1.setEditable(true);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                if (!((AutocompleteField) textField1).getEditMode()) {
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

        label2.setCursor(Cursor.getDefaultCursor());
        label2.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                label2.setIcon(IconLoader.getIcon("/icons/closeIconHovered.svg"));
            }
        });
        addRecursiveMouseListener(label2, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                label2.setIcon(IconLoader.getIcon("/icons/closeIconPressed.svg"));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                label2.setIcon(IconLoader.getIcon("/icons/closeIconHovered.svg"));
            }
        }, () -> {
            removeActiveDecorations();
            return null;
        });

        panel1.setCursor(Cursor.getDefaultCursor());
        panel1.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                panel1.setBackground(Color.decode("#3C3C3C"));
            }
        });

        label4.setCursor(Cursor.getDefaultCursor());

        setCursor(Cursor.getDefaultCursor());
    }

    private void dispose() {
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
        panel1 = new JPanel();
        label1 = new JLabel();
        label3 = new JLabel();
        label4 = new JLabel();
        separator1 = new JSeparator();
        textField1 = new AutocompleteField("$", "Input log message (use $ for variables)", scopeVars, lookup, inlayMark.getLineNumber());
        label2 = new JLabel();

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

        //======== panel1 ========
        {
            panel1.setBackground(new Color(37, 37, 37));
            panel1.setPreferredSize(null);
            panel1.setMinimumSize(null);
            panel1.setMaximumSize(null);
            panel1.setLayout(new MigLayout(
                "fill,insets 0,hidemode 3",
                // columns
                "5[fill]" +
                "[fill]4",
                // rows
                "[grow]"));

            //---- label1 ----
            label1.setIcon(IconLoader.getIcon("/icons/align-left.svg"));
            panel1.add(label1, "cell 0 0");

            //---- label3 ----
            label3.setIcon(IconLoader.getIcon("/icons/angle-down.svg"));
            panel1.add(label3, "cell 1 0");
        }
        add(panel1, "cell 0 0, grow");

        //---- label4 ----
        label4.setIcon(IconLoader.getIcon("/icons/clock.svg"));
        label4.setFont(new Font("Roboto Light", Font.PLAIN, 14));
        label4.setIconTextGap(8);
        label4.setVisible(false);
        add(label4, "cell 1 0,gapx null 8");

        //---- separator1 ----
        separator1.setPreferredSize(new Dimension(5, 20));
        separator1.setMinimumSize(new Dimension(5, 20));
        separator1.setOrientation(SwingConstants.VERTICAL);
        separator1.setMaximumSize(new Dimension(5, 20));
        separator1.setVisible(false);
        add(separator1, "cell 1 0");

        //---- textField1 ----
        textField1.setBackground(new Color(37, 37, 37));
        textField1.setBorder(new CompoundBorder(
                new LineBorder(Color.darkGray, 1, true),
                new EmptyBorder(2, 6, 0, 0)));
        textField1.setFont(new Font("Roboto Light", Font.PLAIN, 14));
        textField1.setMinimumSize(new Dimension(0, 27));
        add(textField1, "cell 2 0");

        //---- label2 ----
        label2.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
        add(label2, "cell 3 0");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel panel1;
    private JLabel label1;
    private JLabel label3;
    private JLabel label4;
    private JSeparator separator1;
    private JTextPane textField1;
    private JLabel label2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
