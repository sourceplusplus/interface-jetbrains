package com.sourceplusplus.sourcemarker.status;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.ui.UIUtil;
import com.sourceplusplus.marker.source.mark.inlay.InlayMark;
import com.sourceplusplus.protocol.artifact.log.Log;
import com.sourceplusplus.protocol.instrument.LiveSourceLocation;
import com.sourceplusplus.protocol.instrument.log.LiveLog;
import com.sourceplusplus.protocol.service.live.LiveInstrumentService;
import com.sourceplusplus.sourcemarker.AutocompletePane;
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys;
import com.sourceplusplus.sourcemarker.psi.LoggerDetector;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
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

import static com.sourceplusplus.protocol.SourceMarkerServices.Instance;

public class LogStatusBar extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a")
            .withZone(ZoneId.systemDefault());
    private Editor editor;
    private final LiveSourceLocation sourceLocation;
    private final List<String> scopeVars;
    private final InlayMark inlayMark;
    private LiveLog liveLog;
    private boolean textFieldFocused = false;
    private boolean editMode;
    private final JLabel label3 = new JLabel();
    private final Function<String, List<String>> lookup;
    private final Pattern VARIABLE_PATTERN;

    public LogStatusBar(LiveSourceLocation sourceLocation, List<String> scopeVars, InlayMark inlayMark) {
        this(sourceLocation, scopeVars, inlayMark, null, null);
    }

    public LogStatusBar(LiveSourceLocation sourceLocation, List<String> scopeVars, InlayMark inlayMark,
                        LiveLog liveLog, Editor editor) {
        this.sourceLocation = sourceLocation;
        this.scopeVars = scopeVars;
        lookup = text -> scopeVars.stream()
                .filter(v -> {
                    String var = substringAfterLast(" ", text);
                    return var.startsWith("$") && v.toLowerCase().contains(var.substring(1))
                            && !v.toLowerCase().equals(var.substring(1));
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
        } else {
            JLabel label3 = new JLabel();
            add(label3, "cell 1 0,gapx null 10");

            //edit mode
            editMode = true;
            textField1.setBorder(new LineBorder(new Color(64, 64, 64), 1, true));
            textField1.setBackground(Color.decode("#252525"));
            textField1.setEditable(true);
        }
    }

    public void setLatestLog(Instant time, Log latestLog) {
        if (textFieldFocused) {
            return; //ignore as they're likely updating text
        }

        String formattedTime = time.atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
        String formattedMessage = latestLog.getFormattedMessage();
        if (!label3.getText().equals(formattedTime) || !textField1.getText().equals(formattedMessage)) {
            SwingUtilities.invokeLater(() -> {
                label3.setText(formattedTime);
                textField1.setText(formattedMessage);

                textField1.getStyledDocument().setCharacterAttributes(
                        0, formattedMessage.length(), textField1.getStyle("default"), true);

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
        //---- label3 ----
        label3.setText("");
        label3.setFont(new Font("Roboto Light", Font.BOLD, 15));
        label3.setIcon(IconLoader.getIcon("/icons/clock.svg"));
        label3.setIconTextGap(8);
        add(label3, "cell 1 0,gapx null 10");

        //---- separator1 ----
        JSeparator separator1 = new JSeparator();
        separator1.setOrientation(SwingConstants.VERTICAL);
        separator1.setPreferredSize(new Dimension(3, 20));
        separator1.setMinimumSize(new Dimension(3, 20));
        separator1.setMaximumSize(new Dimension(3, 20));
        add(separator1, "cell 1 0,gapx 10 10");
    }

    private void removeActiveDecorations() {
        label7.setIcon(IconLoader.getIcon("/icons/expand.svg"));
        label8.setIcon(IconLoader.getIcon("/icons/search.svg"));
        label6.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
        panel1.setBackground(Color.decode("#252525"));

        if (!editMode) {
            textField1.setBorder(null);
            textField1.setBackground(Color.decode("#2B2B2B"));
            textField1.setEditable(false);
        }
    }

    private void addDefaultStyle(JTextPane pn) {
        Style style = pn.addStyle("default", null);
    }

    private void addNumberStyle(JTextPane pn) {
        Style style = pn.addStyle("numbers", null);
        StyleConstants.setForeground(style, Color.decode("#e1483b"));
    }

    private void setupComponents() {
        addDefaultStyle(textField1);
        addNumberStyle(textField1);
        ((AbstractDocument) textField1.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                fb.insertString(offset, string.replaceAll("\\n", ""), attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attr)
                    throws BadLocationException {
                fb.replace(offset, length, string.replaceAll("\\n", ""), attr);
            }
        });
        textField1.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                if (textFieldFocused) {
                    applyStyle();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
            }

            public void applyStyle() {
                SwingUtilities.invokeLater(() -> {
                    String text = textField1.getText();
                    textField1.getStyledDocument().setCharacterAttributes(
                            0, text.length(), textField1.getStyle("default"), true);

                    int minIndex = 0;
                    Matcher m = VARIABLE_PATTERN.matcher(text);
                    while (m.find()) {
                        String var = m.group(1);
                        if (!scopeVars.contains(var.substring(1))) {
                            continue;
                        }

                        int varIndex = text.indexOf(var, minIndex);
                        minIndex = varIndex + var.length();
                        textField1.getStyledDocument().setCharacterAttributes(varIndex, var.length(),
                                textField1.getStyle("numbers"), true);
                    }
                });
            }
        });
        textField1.getDocument().putProperty("filterNewlines", Boolean.TRUE);
        textField1.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
        textField1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_TAB) {
                    //ignore tab; handled by auto-complete pane
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

                    LiveInstrumentService instrumentService = Objects.requireNonNull(Instance.INSTANCE.getLiveInstrument());
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
                            editMode = false;
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
        textField1.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                textFieldFocused = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (editMode) {
                    if (textField1.getText().equals("")) {
                        dispose();
                    }
                } else {
                    textField1.setBorder(null);
                    textField1.setBackground(Color.decode("#2B2B2B"));
                    textField1.setEditable(false);

                    textFieldFocused = false;
                }
            }
        });
        textField1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                textField1.setBorder(new LineBorder(new Color(64, 64, 64), 1, true));
                textField1.setBackground(Color.decode("#252525"));
                textField1.setEditable(true);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                if (!editMode && !textFieldFocused) {
                    textField1.setBorder(null);
                    textField1.setBackground(Color.decode("#2B2B2B"));
                    textField1.setEditable(false);
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                removeActiveDecorations();
            }
        });

        label7.setCursor(Cursor.getDefaultCursor());
        label7.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                label7.setIcon(IconLoader.getIcon("/icons/expandHovered.svg"));
            }
        });
        addRecursiveMouseListener(label7, new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                label7.setIcon(IconLoader.getIcon("/icons/expandPressed.svg"));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                label7.setIcon(IconLoader.getIcon("/icons/expandHovered.svg"));
            }
        });

        label8.setCursor(Cursor.getDefaultCursor());
        label8.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                label8.setIcon(IconLoader.getIcon("/icons/searchHovered.svg"));
            }
        });
        addRecursiveMouseListener(label8, new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                label8.setIcon(IconLoader.getIcon("/icons/searchPressed.svg"));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                label8.setIcon(IconLoader.getIcon("/icons/searchHovered.svg"));
            }
        });

        label6.setCursor(Cursor.getDefaultCursor());
        label6.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                label6.setIcon(IconLoader.getIcon("/icons/closeIconHovered.svg"));
            }
        });
        addRecursiveMouseListener(label6, new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                label6.setIcon(IconLoader.getIcon("/icons/closeIconPressed.svg"));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                label6.setIcon(IconLoader.getIcon("/icons/closeIconHovered.svg"));
            }
        });

        panel1.setCursor(Cursor.getDefaultCursor());
        panel1.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                panel1.setBackground(Color.decode("#3C3C3C"));
            }
        });
    }

    private void dispose() {
        inlayMark.dispose(true, false);

        if (liveLog != null) {
            Instance.INSTANCE.getLiveInstrument().removeLiveInstrument(liveLog.getId(), it -> {
                if (it.succeeded()) {
                    LiveLogStatusManager.INSTANCE.removeActiveLiveLog(liveLog);
                } else {
                    it.cause().printStackTrace();
                }
            });
        }
    }

    public void addRecursiveMouseListener(final Component component, final MouseListener listener) {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) event;

                if (mouseEvent.getComponent() instanceof EditorComponentImpl) {
                    if (event.getID() == MouseEvent.MOUSE_ENTERED) {
                        removeActiveDecorations();
                    }
                } else if (mouseEvent.getComponent().isShowing() && component.isShowing()) {
                    if (containsScreenLocation(component, mouseEvent.getLocationOnScreen())) {
                        if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                            listener.mousePressed(mouseEvent);
                        } else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
                            listener.mouseReleased(mouseEvent);
                        } else if (event.getID() == MouseEvent.MOUSE_ENTERED) {
                            listener.mouseEntered(mouseEvent);
                        } else if (event.getID() == MouseEvent.MOUSE_EXITED) {
                            listener.mouseExited(mouseEvent);
                        } else if (event.getID() == MouseEvent.MOUSE_CLICKED) {
                            listener.mouseClicked(mouseEvent);
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    public static boolean containsScreenLocation(Component component, Point screenLocation) {
        Point compLocation = component.getLocationOnScreen();
        Dimension compSize = component.getSize();
        int relativeX = screenLocation.x - compLocation.x;
        int relativeY = screenLocation.y - compLocation.y;
        return (relativeX >= 0 && relativeX < compSize.width && relativeY >= 0 && relativeY < compSize.height);
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
        label2 = new JLabel();
        textField1 = new AutocompletePane("Input log message (use $ for variables)", lookup);
        panel2 = new JPanel();
        label8 = new JLabel();
        label7 = new JLabel();
        label6 = new JLabel();

        //======== this ========
        setBorder(new LineBorder(new Color(85, 85, 85)));
        setMaximumSize(new Dimension(2147483647, 40));
        setMinimumSize(new Dimension(500, 40));
        setPreferredSize(new Dimension(500, 40));
        setBackground(new Color(43, 43, 43));
        setLayout(new MigLayout(
                "fill,insets 0,hidemode 3",
                // columns
                "[50!]0" +
                        "[fill]0" +
                        "[grow,fill]0" +
                        "[fill]",
                // rows
                "0[grow]0"));

        //======== panel1 ========
        {
            panel1.setBackground(new Color(37, 37, 37));
            panel1.setLayout(new MigLayout(
                    "fill,insets 0,hidemode 3",
                    // columns
                    "[fill]" +
                            "[fill]",
                    // rows
                    "[grow]"));

            //---- label1 ----
            label1.setIcon(IconLoader.getIcon("/icons/align-left.svg"));
            label1.setHorizontalAlignment(SwingConstants.CENTER);
            panel1.add(label1, "pad 0 10 0 0,cell 0 0");

            //---- label2 ----
            label2.setIcon(IconLoader.getIcon("/icons/angle-down.svg"));
            panel1.add(label2, "cell 1 0");
        }
        add(panel1, "cell 0 0,width 50:50:50,height 29:29:29");

        //---- textField1 ----
        textField1.setMinimumSize(new Dimension(0, 0));
        textField1.setBackground(new Color(43, 43, 43));
        textField1.setBorder(null);
        textField1.setEditable(false);
        add(textField1, "cell 2 0");

        //======== panel2 ========
        {
            panel2.setBackground(new Color(43, 43, 43));
            panel2.setLayout(new MigLayout(
                    "btt,hidemode 3",
                    // columns
                    "[fill]5" +
                            "[fill]5" +
                            "[fill]",
                    // rows
                    "[]"));

            //---- label8 ----
            label8.setIcon(IconLoader.getIcon("/icons/search.svg"));
            panel2.add(label8, "cell 0 0");

            //---- label7 ----
            label7.setIcon(IconLoader.getIcon("/icons/expand.svg"));
            label7.setMaximumSize(new Dimension(24, 24));
            label7.setMinimumSize(new Dimension(24, 24));
            label7.setPreferredSize(new Dimension(24, 24));
            panel2.add(label7, "cell 1 0");

            //---- label6 ----
            label6.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
            panel2.add(label6, "cell 2 0");
        }
        add(panel2, "cell 3 0,height 29:29:29");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel panel1;
    private JLabel label1;
    private JLabel label2;
    private JTextPane textField1;
    private JPanel panel2;
    private JLabel label8;
    private JLabel label7;
    private JLabel label6;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
