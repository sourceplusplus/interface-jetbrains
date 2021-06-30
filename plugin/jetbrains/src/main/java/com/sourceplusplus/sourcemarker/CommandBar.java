package com.sourceplusplus.sourcemarker;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.UIUtil;
import com.sourceplusplus.marker.source.mark.inlay.InlayMark;
import com.sourceplusplus.sourcemarker.command.CommandBarController;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sourceplusplus.sourcemarker.status.LogStatusBar.containsScreenLocation;

public class CommandBar extends JPanel {

    private final List<String> commands = Stream.of("/add-live-log", "/add-live-breakpoint")
            .sorted().collect(Collectors.toList());
    private final Function<String, List<String>> lookup = text -> commands.stream()
            .filter(v -> !text.isEmpty()
                    && v.toLowerCase().contains(text.toLowerCase().replace("/", ""))
                    && text.startsWith("/")
            ).collect(Collectors.toList());

    private final InlayMark inlayMark;
    private Editor editor;

    public CommandBar(InlayMark inlayMark) {
        this.inlayMark = inlayMark;

        initComponents();
        setupComponents();
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
                        CommandBarController.INSTANCE.handleCommandInput(autoCompleteText, inlayMark, editor);
                    } else {
                        CommandBarController.INSTANCE.handleCommandInput(textField1.getText(), inlayMark, editor);
                    }
                }
            }
        });
        textField1.addActionListener(it -> {
            CommandBarController.INSTANCE.handleCommandInput(textField1.getText(), inlayMark, editor);
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

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        label1 = new JLabel();
        textField1 = new AutocompleteField("Search or Type a Command (/)", lookup);
        label2 = new JLabel();

        //======== this ========
        setPreferredSize(new Dimension(500, 40));
        setMinimumSize(new Dimension(500, 40));
        setBorder(new LineBorder(new Color(85, 85, 85)));
        setBackground(new Color(43, 43, 43));
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "[fill]" +
            "[grow,fill]" +
            "[fill]",
            // rows
            "0[grow]0"));

        //---- label1 ----
        label1.setIcon(IconLoader.getIcon("/icons/command/logo.svg"));
        add(label1, "cell 0 0");

        //---- textField1 ----
        textField1.setBackground(new Color(37, 37, 37));
        textField1.setBorder(new LineBorder(Color.darkGray, 1, true));
        textField1.setFont(new Font("Roboto Light", Font.PLAIN, 14));
        add(textField1, "cell 1 0");

        //---- label2 ----
        label2.setIcon(IconLoader.getIcon("/icons/closeIcon.svg"));
        add(label2, "cell 2 0");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JLabel label1;
    private AutocompleteField textField1;
    private JLabel label2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
