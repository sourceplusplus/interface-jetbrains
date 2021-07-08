package com.sourceplusplus.sourcemarker.status.util

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.function.Function
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AutocompleteField(
    private val placeHolderText: String?,
    private val lookup: Function<String, List<String>>
) : JTextField(), FocusListener, DocumentListener, KeyListener {

    private val results: MutableList<String>
    private val popup: JWindow
    private val list: JList<String>
    private val model: ListModel<String>

    init {
        results = ArrayList()
        popup = JWindow(SwingUtilities.getWindowAncestor(this))
        popup.type = Window.Type.POPUP
        popup.focusableWindowState = false
        popup.isAlwaysOnTop = true
        model = ListModel()
        list = JBList(model)
        list.setBackground(JBColor.decode("#252525"))
        list.setBorder(JBUI.Borders.empty())
        val scroll: JScrollPane = object : JScrollPane(list) {
            override fun getPreferredSize(): Dimension {
                val ps = super.getPreferredSize()
                ps.width = this@AutocompleteField.width
                return ps
            }
        }
        scroll.border = JBUI.Borders.empty()
        popup.add(scroll)
        addFocusListener(this)
        document.addDocumentListener(this)
        addKeyListener(this)
    }

    private fun showAutocompletePopup() {
        popup.setLocation(locationOnScreen.x, locationOnScreen.y + height + 6)
        popup.isVisible = true
    }

    private fun hideAutocompletePopup() {
        popup.isVisible = false
    }

    override fun focusGained(e: FocusEvent) = SwingUtilities.invokeLater {
        if (results.size > 0) {
            showAutocompletePopup()
        }
    }

    override fun focusLost(e: FocusEvent) = SwingUtilities.invokeLater { hideAutocompletePopup() }

    private fun documentChanged() = SwingUtilities.invokeLater {
        // Updating results list
        results.clear()
        results.addAll(lookup.apply(text))

        // Updating list view
        model.updateView()
        list.visibleRowCount = results.size.coerceAtMost(10)

        // Selecting first result
        if (results.size > 0) {
            list.selectedIndex = 0
        }

        // Ensure autocomplete popup has correct size
        popup.pack()

        // Display or hide popup depending on the results
        if (results.size > 0) {
            showAutocompletePopup()
        } else {
            hideAutocompletePopup()
        }
    }

    override fun getSelectedText(): String? = list.selectedValue

    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_UP) {
            val index = list.selectedIndex
            if (index > 0) {
                list.selectedIndex = index - 1
            }
        } else if (e.keyCode == KeyEvent.VK_DOWN) {
            val index = list.selectedIndex
            if (index != -1 && list.model.size > index + 1) {
                list.selectedIndex = index + 1
            }
        } else if (e.keyCode == KeyEvent.VK_TAB) {
            val text = list.selectedValue
            setText(text)
            caretPosition = text.length
        }
    }

    override fun insertUpdate(e: DocumentEvent) = documentChanged()
    override fun removeUpdate(e: DocumentEvent) = documentChanged()
    override fun changedUpdate(e: DocumentEvent) = documentChanged()
    override fun keyTyped(e: KeyEvent) = Unit
    override fun keyReleased(e: KeyEvent) = Unit

    private inner class ListModel<T> : AbstractListModel<T>() {
        override fun getSize(): Int = results.size
        override fun getElementAt(index: Int): T = results[index] as T
        fun updateView() = fireContentsChanged(this@AutocompleteField, 0, size)
    }

    override fun paintComponent(pG: Graphics) {
        super.paintComponent(pG)
        if (text.isNotEmpty() || placeHolderText == null) {
            return
        }

        val g = pG as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

//        //        g.setColor(Color.decode("#252525"));
////        g.drawString(placeHolderText, getInsets().left + 6, pG.getFontMetrics()
////                .getMaxAscent() + getInsets().top + 2);
//        g.setColor(new Color(225, 72, 59));
//        g.drawString(placeHolderText.substring(6), ((float) g.getFontMetrics().getStringBounds(placeHolderText.substring(0, 6), g).getWidth()) + ((float) getInsets().left + 6), pG.getFontMetrics()
//                .getMaxAscent() + getInsets().top + 2);

        g.color = Color(85, 85, 85, 200)
        g.drawString(
            placeHolderText, insets.left + 6,
            pG.getFontMetrics().maxAscent + insets.top + 2
        )
    }
}
