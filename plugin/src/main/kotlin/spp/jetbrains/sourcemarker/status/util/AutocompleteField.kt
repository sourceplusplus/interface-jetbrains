package spp.jetbrains.sourcemarker.status.util

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import spp.jetbrains.sourcemarker.PluginIcons
import spp.jetbrains.sourcemarker.PluginUI.*
import spp.jetbrains.sourcemarker.service.log.VariableParser
import spp.jetbrains.sourcemarker.command.AutocompleteFieldRow
import java.awt.*
import java.awt.event.*
import java.util.function.Function
import java.util.regex.Matcher
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
    private val lookup: Function<String, List<AutocompleteFieldRow>>? = null,
    internal val lineNumber: Int = 0,
    private val replaceCommandOnTab: Boolean = false,
    private val autocompleteOnEnter: Boolean = true,
    private val varColor: Color = COMPLETE_COLOR_PURPLE
) : JTextPane(), FocusListener, DocumentListener, KeyListener, MouseMotionListener, MouseListener {

    private val results: MutableList<AutocompleteFieldRow>
    private val popup: JWindow
    private val list: JList<AutocompleteFieldRow>
    private val model: ListModel<AutocompleteFieldRow>
    var editMode: Boolean = true
    private var showSaveButton: Boolean = false
    private val listeners: MutableList<SaveListener> = mutableListOf()
    private var hasControlHeld = false
    var saveOnSuggestionDoubleClick: Boolean = false
    var addOnSuggestionDoubleClick: Boolean = true
    var placeHolderTextColor: Color? = null
    var canShowSaveButton = true
    var patternPair: Pair<Pattern?, Pattern?> = Pair.empty();

    val matchAndApplyStyle = { m: Matcher ->
        while (m.find()) {
            val variable: String = m.group(1)
            val varIndex = m.start()
            styledDocument.setCharacterAttributes(varIndex, variable.length, getStyle("numbers"), true)
        }
    }

    init {
        foreground = UIUtil.getTextFieldForeground()

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

        list.font = ROBOTO_LIGHT_PLAIN_14
        list.setCellRenderer(AutoCompleteCellRenderer(lineNumber))

        list.setBackground(AUTO_COMPLETE_HIGHLIGHT_COLOR)
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

        patternPair = VariableParser.createPattern(allLookup.map { a->a.getText().substring(1)})

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

    fun isPopupVisible(): Boolean {
        return popup.isVisible
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

        VariableParser.matchVariables(patternPair, text, matchAndApplyStyle)
    }

    private fun addNumberStyle(pn: JTextPane) {
        val style = pn.addStyle("numbers", null)
        StyleConstants.setForeground(style, varColor)
    }

    fun showAutocompletePopup() {
        try {
            popup.pack()
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
        lookup?.let { results.addAll(it.apply(text)) }
        model.updateView()
        list.visibleRowCount = results.size.coerceAtMost(10)
        if (results.size > 0) {
            list.selectedIndex = 0
        }

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

            if (results.size > 0) {
                showAutocompletePopup()
            } else {
                hideAutocompletePopup()
            }
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

    private val saveIcon = (PluginIcons.Instrument.save as ScalableIcon)
        .scale(0.40f)
    private val saveHoveredIcon = (PluginIcons.Instrument.saveHovered as ScalableIcon)
        .scale(0.40f)
    private val savePressedIcon = (PluginIcons.Instrument.savePressed as ScalableIcon)
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
        if (canShowSaveButton && showSaveButton && paintIcon != null) {
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

            g.color = placeHolderTextColor ?: Color(
                UIUtil.getTextFieldForeground().red,
                UIUtil.getTextFieldForeground().green,
                UIUtil.getTextFieldForeground().blue,
                100
            )
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
