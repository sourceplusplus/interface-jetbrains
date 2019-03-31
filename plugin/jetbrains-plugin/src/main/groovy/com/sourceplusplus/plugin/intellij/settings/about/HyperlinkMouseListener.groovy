package com.sourceplusplus.plugin.intellij.settings.about

import javax.swing.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

/**
 * todo: description
 *
 * @version 0.1.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class HyperlinkMouseListener implements MouseListener {

    private final JLabel label
    private final String link

    HyperlinkMouseListener(JLabel label, String link) {
        this.label = label
        this.link = link
    }

    void mouseClicked(MouseEvent arg0) {
        try {
            Desktop.getDesktop().browse(URI.create(link))
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    void mouseExited(MouseEvent arg0) {
    }

    void mouseEntered(MouseEvent arg0) {
    }

    void mousePressed(MouseEvent e) {
    }

    void mouseReleased(MouseEvent e) {
    }
}
