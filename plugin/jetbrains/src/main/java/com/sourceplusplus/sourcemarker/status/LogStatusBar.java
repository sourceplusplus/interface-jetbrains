package com.sourceplusplus.sourcemarker.status;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.ui.UIUtil;
import com.sourceplusplus.marker.source.mark.inlay.InlayMark;
import com.sourceplusplus.protocol.instrument.LiveSourceLocation;
import com.sourceplusplus.protocol.instrument.log.LiveLog;
import com.sourceplusplus.protocol.service.live.LiveInstrumentService;
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys;
import com.sourceplusplus.sourcemarker.psi.LoggerDetector;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sourceplusplus.protocol.SourceMarkerServices.Instance;

public class LogStatusBar extends JPanel {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("(\\$[a-zA-Z0-9_]+)");
    private Editor editor;
    private final LiveSourceLocation sourceLocation;
    private final InlayMark inlayMark;
    private LiveLog liveLog;
    private boolean textFieldFocused = false;
    private boolean editMode;

    public LogStatusBar(LiveSourceLocation sourceLocation, InlayMark inlayMark) {
        this(sourceLocation, inlayMark, null, null);
    }

    public LogStatusBar(LiveSourceLocation sourceLocation, InlayMark inlayMark, LiveLog liveLog, Editor editor) {
        this.sourceLocation = sourceLocation;
        this.inlayMark = inlayMark;
        this.liveLog = liveLog;
        this.editor = editor;

        initComponents();
        setupComponents();

        if (liveLog != null) {
            //---- label3 ----
            JLabel label3 = new JLabel();
            label3.setText("5:45:22pm");
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

            textField1.setText(liveLog.getLogFormat()); //todo: log message
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

    public void setEditor(Editor editor) {
        this.editor = editor;
    }

    public void focus() {
        textField1.grabFocus();
        textField1.requestFocusInWindow();
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

    private void setupComponents() {
        textField1.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
        textField1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (textField1.getText().equals("")) {
                    return;
                }

                String logPattern = textField1.getText();
                ArrayList<String> varMatches = new ArrayList<>();
                Matcher m = VARIABLE_PATTERN.matcher(logPattern);
                while (m.find()) {
                    String var = m.group();
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
                        editMode = false;
                        removeActiveDecorations();

                        LoggerDetector detector = inlayMark.getUserData(
                                SourceMarkKeys.INSTANCE.getLOGGER_DETECTOR()
                        );
                        detector.addLiveLog(editor, inlayMark, finalLogPattern, sourceLocation.getLine());
                        liveLog = (LiveLog) it.result();
                        LiveLogStatusManager.INSTANCE.addActiveLiveLog(liveLog);

                        //focus back to IDE
                        IdeFocusManager.getInstance(editor.getProject())
                                .requestFocusInProject(editor.getContentComponent(), editor.getProject());
                    } else {
                        it.cause().printStackTrace();
                    }
                });
            }
        });
        textField1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    inlayMark.dispose();
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
                        inlayMark.dispose();
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
                inlayMark.dispose();

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

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        panel1 = new JPanel();
        label1 = new JLabel();
        label2 = new JLabel();
        label4 = new JLabel();
        textField1 = new JTextField();
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
//        setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.TitledBorder(new javax.swing.
//        border.EmptyBorder(0,0,0,0), "JF\u006frmDes\u0069gner \u0045valua\u0074ion",javax.swing.border.TitledBorder.CENTER
//        ,javax.swing.border.TitledBorder.BOTTOM,new java.awt.Font("D\u0069alog",java.awt.Font
//        .BOLD,12),java.awt.Color.red), getBorder())); addPropertyChangeListener(
//        new java.beans.PropertyChangeListener(){@Override public void propertyChange(java.beans.PropertyChangeEvent e){if("\u0062order"
//        .equals(e.getPropertyName()))throw new RuntimeException();}});
        setLayout(new MigLayout(
                "fill,insets 0,hidemode 3",
                // columns
                "[50!]0" +
                        "[fill]0" +
                        "[40!,fill]-5" +
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

        //---- label4 ----
        label4.setIcon(IconLoader.getIcon("/icons/info.svg"));
        add(label4, "cell 2 0");

        //---- textField1 ----
        textField1.setBackground(new Color(43, 43, 43));
        textField1.setBorder(null);
        textField1.setEditable(false);
        add(textField1, "cell 3 0");

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
        add(panel2, "cell 4 0,height 29:29:29");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel panel1;
    private JLabel label1;
    private JLabel label2;
    private JLabel label4;
    private JTextField textField1;
    private JPanel panel2;
    private JLabel label8;
    private JLabel label7;
    private JLabel label6;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
