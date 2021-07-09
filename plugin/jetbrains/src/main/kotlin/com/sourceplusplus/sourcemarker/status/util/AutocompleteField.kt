package com.sourceplusplus.sourcemarker.status.util

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionItemPriority
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.function.Function
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.*


class AutocompleteField(
    private val placeHolderText: String?,
    private val allLookup: List<String>,
    private val lookup: Function<String, List<String>>
) : JTextPane(), FocusListener, DocumentListener, KeyListener {

    private val results: MutableList<String>
    private val popup: JWindow
    private val list: JList<String>
    private val model: ListModel<String>
    private val variablePattern: Pattern

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

        val sb = StringBuilder("(")
        for (i in allLookup.indices) {
            sb.append(Regex.escape(allLookup[i]))
            if (i + 1 < allLookup.size) {
                sb.append("|")
            }
        }
        sb.append(")(?:\\s|$)")
        variablePattern = Pattern.compile(sb.toString())

        document.putProperty("filterNewlines", SmartCompletionItemPriority.TRUE)

        addNumberStyle(this)

        (document as AbstractDocument).documentFilter = object : DocumentFilter() {
            override fun insertString(fb: FilterBypass, offset: Int, string: String, attr: AttributeSet) =
                fb.insertString(offset, string.replace("\\n".toRegex(), ""), attr)

            override fun replace(fb: FilterBypass, offset: Int, length: Int, string: String, attr: AttributeSet) =
                fb.replace(offset, length, string.replace("\\n".toRegex(), ""), attr)
        }
    }

    private fun applyStyle() {
        styledDocument.setCharacterAttributes(
            0,
            text.length,
            StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE),
            true
        )

        var minIndex = 0
        val m = variablePattern.matcher(text)
        while (m.find()) {
            val variable: String = m.group(1)
            val varIndex = text.indexOf(variable, minIndex)
            minIndex = varIndex + variable.length
            styledDocument.setCharacterAttributes(varIndex, variable.length, getStyle("numbers"), false)
        }
    }

    private fun addNumberStyle(pn: JTextPane) {
        val style = pn.addStyle("numbers", null)
        StyleConstants.setForeground(style, Color.decode("#e1483b"))
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

    override fun keyTyped(e: KeyEvent) = Unit

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
        } else if (e.keyCode == KeyEvent.VK_TAB || e.keyCode == KeyEvent.VK_ENTER) {
            val text = list.selectedValue
            if (text != null) {
                val varCompleted = getText().substringAfterLast("$")
                setText(getText() + text.substring(varCompleted.length))
                caretPosition = getText().length
            }
        }
    }

    override fun keyReleased(e: KeyEvent) {
        if (e.keyCode != KeyEvent.VK_UP && e.keyCode != KeyEvent.VK_DOWN) {
            applyStyle()
        }
    }

    override fun insertUpdate(e: DocumentEvent) = documentChanged()
    override fun removeUpdate(e: DocumentEvent) = documentChanged()
    override fun changedUpdate(e: DocumentEvent) = documentChanged()

    private inner class ListModel<T> : AbstractListModel<T>() {
        override fun getSize(): Int = results.size
        override fun getElementAt(index: Int): T = results[index] as T
        fun updateView() = fireContentsChanged(this@AutocompleteField, 0, size)
    }

    override fun paintComponent(pG: Graphics) {
        super.paintComponent(pG)

        if (text.isEmpty() && placeHolderText != null) {
            val g = pG as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            g.color = Color(85, 85, 85, 200)
            g.drawString(placeHolderText, insets.left, pG.getFontMetrics().maxAscent + insets.top)
        }
    }
}
