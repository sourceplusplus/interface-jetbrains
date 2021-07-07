package com.sourceplusplus.sourcemarker.status;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.UIUtil;
import com.sourceplusplus.marker.source.mark.inlay.InlayMark;
import com.sourceplusplus.protocol.artifact.log.Log;
import com.sourceplusplus.protocol.instrument.LiveSourceLocation;
import com.sourceplusplus.protocol.instrument.log.LiveLog;
import com.sourceplusplus.sourcemarker.AutocompleteField;
import com.sourceplusplus.sourcemarker.command.CommandBarController;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sourceplusplus.sourcemarker.status.ViewUtils.containsScreenLocation;

public class LogStatusBar extends JPanel {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a")
            .withZone(ZoneId.systemDefault());
    private final InlayMark inlayMark;
    private final LiveSourceLocation sourceLocation;
    private final List<String> scopeVars;
    private final Function<String, List<String>> lookup;
    private final Pattern VARIABLE_PATTERN;
    private Editor editor;
    private LiveLog liveLog;

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
    }

    public void setLatestLog(Instant time, Log latestLog) {

    }

    public void setEditor(Editor editor) {
        this.editor = editor;
    }

    public void focus() {
        textField1.grabFocus();
        textField1.requestFocusInWindow();
    }

    private void setupComponents() {
        textField1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    String autoCompleteText = textField1.getSelectedText();
                    if (autoCompleteText != null) {
                        CommandBarController.INSTANCE.handleCommandInput(autoCompleteText, editor);
                    } else {
                        CommandBarController.INSTANCE.handleCommandInput(textField1.getText(), editor);
                    }
                }
            }
        });
        textField1.addActionListener(it -> {
            CommandBarController.INSTANCE.handleCommandInput(textField1.getText(), editor);
        });
        textField1.setFocusTraversalKeysEnabled(false);
        textField1.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true);
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
        });
    }

    private void dispose() {
        inlayMark.dispose(true, false);
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

    private void removeActiveDecorations() {
        label2.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
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
        textField1 = new AutocompleteField("Input log message (use $ for variables)", lookup);
        label2 = new JLabel();

        //======== this ========
        setPreferredSize(new Dimension(500, 40));
        setMinimumSize(new Dimension(500, 40));
        setBorder(new LineBorder(new Color(85, 85, 85)));
        setBackground(new Color(43, 43, 43));
//        setBorder ( new javax . swing. border .CompoundBorder ( new javax . swing. border .TitledBorder ( new javax . swing. border .
//        EmptyBorder ( 0, 0 ,0 , 0) ,  "JF\u006frmD\u0065sig\u006eer \u0045val\u0075ati\u006fn" , javax. swing .border . TitledBorder. CENTER ,javax . swing
//        . border .TitledBorder . BOTTOM, new java. awt .Font ( "Dia\u006cog", java .awt . Font. BOLD ,12 ) ,
//        java . awt. Color .red ) , getBorder () ) );  addPropertyChangeListener( new java. beans .PropertyChangeListener ( )
//        { @Override public void propertyChange (java . beans. PropertyChangeEvent e) { if( "\u0062ord\u0065r" .equals ( e. getPropertyName () ) )
//        throw new RuntimeException( ) ;} } );
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
        label4.setText("time goes here");
        add(label4, "cell 1 0,gapx null 8");

        //---- separator1 ----
        separator1.setPreferredSize(new Dimension(5, 20));
        separator1.setMinimumSize(new Dimension(5, 20));
        separator1.setOrientation(SwingConstants.VERTICAL);
        separator1.setMaximumSize(new Dimension(5, 20));
        add(separator1, "cell 1 0");

        //---- textField1 ----
        textField1.setBackground(new Color(37, 37, 37));
        textField1.setBorder(new LineBorder(Color.darkGray, 1, true));
        textField1.setFont(new Font("Roboto Light", Font.PLAIN, 14));
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
    private JTextField textField1;
    private JLabel label2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
