package com.sourceplusplus.sourcemarker.status.util

import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.sourceplusplus.sourcemarker.command.AutocompleteFieldRow
import java.awt.*
import java.awt.event.*
import java.util.function.Function
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.*

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class AutocompleteField(
    var placeHolderText: String?,
    private val allLookup: List<AutocompleteFieldRow>,
    private val lookup: Function<String, List<AutocompleteFieldRow>>,
    internal val lineNumber: Int = 0,
    private val replaceCommandOnTab: Boolean = false,
    private val autocompleteOnEnter: Boolean = true,
    private val varColor: Color = Color.decode("#9876AA")
) : JTextPane(), FocusListener, DocumentListener, KeyListener, MouseMotionListener, MouseListener {

    private val results: MutableList<AutocompleteFieldRow>
    private val popup: JWindow
    private val list: JList<AutocompleteFieldRow>
    private val model: ListModel<AutocompleteFieldRow>
    private val variablePattern: Pattern
    var editMode: Boolean = true
    private var showSaveButton: Boolean = false
    private val listeners: MutableList<SaveListener> = mutableListOf()
    private var hasControlHeld = false
    var saveOnSuggestionDoubleClick: Boolean = false
    var addOnSuggestionDoubleClick: Boolean = true
    var placeHolderTextColor: Color = Color(85, 85, 85, 200)

    init {
        foreground = Color.decode("#A9B7C6")

        results = ArrayList()
        popup = JWindow(SwingUtilities.getWindowAncestor(this))
        popup.type = Window.Type.POPUP
        popup.focusableWindowState = false
        popup.isAlwaysOnTop = true
        model = ListModel()
        list = JBList(model)
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    if (saveOnSuggestionDoubleClick) {
                        listeners.forEach(SaveListener::onSave)
                    } else if (addOnSuggestionDoubleClick) {
                        addAutoCompleteToInput(list.selectedValue)
                    }
                }
            }
        })

        list.font = Font("Roboto Light", Font.PLAIN, 14)
        list.setCellRenderer(AutoCompleteCellRenderer(lineNumber))

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
            sb.append(Regex.escape(allLookup[i].getText()))
            if (i + 1 < allLookup.size) {
                sb.append("|")
            }
        }
        sb.append(")(?:\\s|$)")
        variablePattern = Pattern.compile(sb.toString())

        document.putProperty("filterNewlines", true)

        addNumberStyle(this)

        (document as AbstractDocument).documentFilter = object : DocumentFilter() {
            override fun insertString(fb: FilterBypass, offset: Int, string: String, attr: AttributeSet?) =
                fb.insertString(offset, string.replace("\\n".toRegex(), ""), attr)

            override fun replace(fb: FilterBypass, offset: Int, length: Int, string: String, attr: AttributeSet?) =
                fb.replace(offset, length, string.replace("\\n".toRegex(), ""), attr)
        }

        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(documentEvent: DocumentEvent) {
                if (editMode) {
                    SwingUtilities.invokeLater {
                        applyStyle()
                    }
                }
            }

            override fun removeUpdate(documentEvent: DocumentEvent) {
                if (editMode) {
                    SwingUtilities.invokeLater {
                        applyStyle()
                    }
                }
            }

            override fun changedUpdate(documentEvent: DocumentEvent) = Unit
        })

        addMouseListener(this)
        addMouseMotionListener(this)
    }

    fun setCellRenderer(listCellRenderer: DefaultListCellRenderer) {
        list.cellRenderer = listCellRenderer
    }

    fun setShowSaveButton(showSaveButton: Boolean) {
        this.showSaveButton = showSaveButton
        icon = (if (showSaveButton) saveIcon else null)

        repaint()
    }

    fun isShowingSaveButton(): Boolean {
        return showSaveButton
    }

    fun addSaveListener(listener: SaveListener) {
        listeners.add(listener)
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
            styledDocument.setCharacterAttributes(varIndex, variable.length, getStyle("numbers"), true)
        }
    }

    private fun addNumberStyle(pn: JTextPane) {
        val style = pn.addStyle("numbers", null)
        StyleConstants.setForeground(style, varColor)
    }

    fun showAutocompletePopup() {
        try {
            popup.setLocation(locationOnScreen.x, locationOnScreen.y + height + 6)
            popup.isVisible = true
        } catch (ignored: IllegalComponentStateException) {
            //trying to open popup too soon/late
        }
    }

    fun hideAutocompletePopup() {
        popup.isVisible = false
    }

    override fun focusGained(e: FocusEvent) = Unit
    override fun focusLost(e: FocusEvent) = SwingUtilities.invokeLater { hideAutocompletePopup() }

    private fun documentChanged() = SwingUtilities.invokeLater {
        if (!editMode) return@invokeLater
        results.clear()
        results.addAll(lookup.apply(text))
        model.updateView()
        list.visibleRowCount = results.size.coerceAtMost(10)
        if (results.size > 0) {
            list.selectedIndex = 0
        }
        popup.pack()

        if (text.isNotEmpty() && results.size > 0) {
            showAutocompletePopup()
        } else {
            hideAutocompletePopup()
        }
    }

    override fun getSelectedText(): String? = list.selectedValue?.getText()

    override fun keyTyped(e: KeyEvent) = Unit

    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_SPACE && hasControlHeld) {
            results.clear()
            results.addAll(allLookup)
            model.updateView()
            list.visibleRowCount = results.size.coerceAtMost(10)
            if (results.size > 0) {
                list.selectedIndex = 0
            }
            popup.pack()

            showAutocompletePopup()
        } else if (e.keyCode == KeyEvent.VK_CONTROL) {
            hasControlHeld = true
        } else if (e.keyCode == KeyEvent.VK_UP) {
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
            if (e.keyCode == KeyEvent.VK_ENTER && !autocompleteOnEnter) {
                hideAutocompletePopup()
                return
            }

            val text = list.selectedValue
            if (text != null) {
                if (replaceCommandOnTab) {
                    setText(text.getText())
                    caretPosition = getText().length
                } else {
                    addAutoCompleteToInput(text)
                }
            }
        }
    }

    private fun addAutoCompleteToInput(text: AutocompleteFieldRow) {
        if (getText().isEmpty()) {
            setText(list.selectedValue.getText())
        } else {
            setText(
                getText().substring(0, getText().lastIndexOf(getText().substringAfterLast(" ")))
                        + text.getText()
            )
        }
        caretPosition = getText().length
    }

    override fun keyReleased(e: KeyEvent) {
        if (hasControlHeld && e.keyCode == KeyEvent.VK_CONTROL) {
            hasControlHeld = false
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

    private val saveIcon = (IconLoader.findIcon("/icons/instrument/live-log/save.svg") as ScalableIcon)
        .scale(0.40f)
    private val saveHoveredIcon = (IconLoader.findIcon("/icons/instrument/live-log/saveHovered.svg") as ScalableIcon)
        .scale(0.40f)
    private val savePressedIcon = (IconLoader.findIcon("/icons/instrument/live-log/savePressed.svg") as ScalableIcon)
        .scale(0.40f)
    private var icon: Icon? = saveIcon
    private var iconX: Int = 0
    private var iconY: Int = 0
    private var iconWidth: Int = 0
    private var iconHeight: Int = 0
    private var iconRect: Rectangle = Rectangle(0, 0, 0, 0)

    override fun paintComponent(pG: Graphics) {
        super.paintComponent(pG)
        val g = pG as Graphics2D
        val paintIcon = icon
        if (showSaveButton && paintIcon != null) {
            iconX = width - paintIcon.iconWidth - 3
            iconY = 4
            iconWidth = paintIcon.iconWidth
            iconHeight = paintIcon.iconHeight
            iconRect = Rectangle(iconX, iconY, iconWidth, iconHeight)
            paintIcon.paintIcon(null, g, iconX, iconY)
        }

        if (text.isEmpty() && placeHolderText != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val textLength = pG.getFontMetrics().stringWidth(placeHolderText)
            val fieldLength = width

            g.color = placeHolderTextColor
            g.drawString(
                placeHolderText,
                insets.left + (fieldLength / 2) - (textLength / 2),
                pG.getFontMetrics().maxAscent + insets.top
            )
        }
    }

    override fun mouseDragged(e: MouseEvent) {
        val previousIcon = icon
        if (iconRect.contains(Point(e.x, e.y))) {
            cursor = Cursor.getDefaultCursor()
            icon = savePressedIcon

            if (previousIcon !== icon) repaint()
        } else {
            cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
            icon = saveIcon

            if (previousIcon !== icon) repaint()
        }
    }

    override fun mouseMoved(e: MouseEvent) {
        val previousIcon = icon
        if (iconRect.contains(Point(e.x, e.y))) {
            cursor = Cursor.getDefaultCursor()
            icon = saveHoveredIcon

            if (previousIcon !== icon) repaint()
        } else {
            cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
            icon = saveIcon

            if (previousIcon !== icon) repaint()
        }
    }

    override fun mouseClicked(e: MouseEvent) {
        if (showSaveButton && iconRect.contains(Point(e.x, e.y))) {

            listeners.forEach(SaveListener::onSave)
            repaint()
        }
    }

    override fun mousePressed(e: MouseEvent) {
        val previousIcon = icon
        if (iconRect.contains(Point(e.x, e.y))) {
            icon = savePressedIcon

            if (previousIcon !== icon) repaint()
        }
    }

    override fun mouseReleased(e: MouseEvent) {
        val previousIcon = icon
        if (iconRect.contains(Point(e.x, e.y))) {
            icon = saveHoveredIcon

            if (previousIcon !== icon) repaint()
        }
    }

    override fun mouseEntered(e: MouseEvent) = Unit
    override fun mouseExited(e: MouseEvent) = Unit

    fun interface SaveListener {
        fun onSave()
    }
}
