/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.command.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import spp.jetbrains.PluginUI
import spp.jetbrains.PluginUI.SMALLEST_FONT
import spp.jetbrains.PluginUI.commandHighlightForeground
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.instrument.log.VariableParser
import spp.jetbrains.marker.SourceMarkerUtils.substringAfterIgnoreCase
import spp.jetbrains.sourcemarker.command.status.ui.element.AutocompleteDropdown
import spp.jetbrains.sourcemarker.command.ui.util.LiveCommandFieldRow
import spp.protocol.artifact.ArtifactQualifiedName
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
@Suppress("MagicNumber", "TooManyFunctions")
class AutocompleteField<T : AutocompleteFieldRow>(
    val project: Project,
    var placeHolderText: String?,
    private val allLookup: List<T>,
    private val lookup: Function<String, List<T>>? = null,
    internal val artifactQualifiedName: ArtifactQualifiedName,
    addAutocompleteDropdownInfo: Boolean = false
) : JTextPane(), FocusListener, DocumentListener, KeyListener, MouseMotionListener, MouseListener {

    var autocompleteOnTab: Boolean = true
    var replaceCommandOnTab: Boolean = false
    var autocompleteAndFinishOnEnter: Boolean = false
    var varColor: JBColor = PluginUI.commandHighlightForeground
    private val results: MutableList<AutocompleteFieldRow>
    private val autocompleteDropdown: AutocompleteDropdown?
    private val popup: JWindow
    private val list: JBList<AutocompleteFieldRow>
    private val model: ListModel<AutocompleteFieldRow>
    var editMode: Boolean = true
    private var showSaveButton: Boolean = false
    private val listeners: MutableList<SaveListener> = mutableListOf()
    private var hasControlHeld = false
    var saveOnSuggestionDoubleClick: Boolean = false
    var addOnSuggestionDoubleClick: Boolean = true
    var placeHolderTextColor: JBColor? = null
    var canShowSaveButton = true
    var varPattern: Pattern = Pattern.compile("")
    var includeCurlyPattern: Boolean = false
    var actualText: String = ""
        private set
    var ready: Boolean = false
        private set
    var maxSuggestSize = 10

    private val matchAndApplyStyle = { m: Matcher ->
        if (varPattern.pattern().isNotEmpty()) {
            while (m.find()) {
                val variable: String = m.group(1)
                val varIndex = m.start()
                styledDocument.setCharacterAttributes(varIndex, variable.length, getStyle("numbers"), true)
            }
        }
    }

    init {
        foreground = PluginUI.getBackgroundFocusColor()

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

        list.font = SMALLEST_FONT
        list.cellRenderer = AutocompleteCellRenderer(artifactQualifiedName)

        list.border = JBUI.Borders.empty()
        val scroll: JBScrollPane = object : JBScrollPane(list) {
            override fun getPreferredSize(): Dimension {
                val ps = super.getPreferredSize()
                ps.width = this@AutocompleteField.width
                return ps
            }
        }
        scroll.border = JBUI.Borders.empty()

        if (addAutocompleteDropdownInfo) {
            autocompleteDropdown = AutocompleteDropdown(project).apply { setScrollPane(scroll) }
            autocompleteDropdown.setTotalCommandsLabel(allLookup.size)
            popup.add(autocompleteDropdown)
        } else {
            autocompleteDropdown = null
            popup.add(scroll)
        }

        addFocusListener(this)
        document.addDocumentListener(this)
        addKeyListener(this)

        varPattern = VariableParser.createPattern(allLookup.map { it.getText() }, "", includeCurlyPattern, true)

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

        VariableParser.matchVariables(varPattern, text, matchAndApplyStyle)
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
        list.visibleRowCount = results.size.coerceAtMost(maxSuggestSize)
        autocompleteDropdown?.setCurrentCommandsLabel(list.visibleRowCount)
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
            results.addAll(allLookup
                .filter { it.getText().lowercase().contains(text) }
                .sortedBy { it.getText() })
            model.updateView()
            list.visibleRowCount = results.size.coerceAtMost(maxSuggestSize)
            autocompleteDropdown?.setCurrentCommandsLabel(list.visibleRowCount)
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
            scrollListToSelected()
        } else if (e.keyCode == KeyEvent.VK_DOWN) {
            val index = list.selectedIndex
            if (index != -1 && list.model.size > index + 1) {
                list.selectedIndex = index + 1
            }
            scrollListToSelected()
        } else if (e.keyCode == KeyEvent.VK_TAB) {
            if (text.isBlank() || list.selectedValue == null || (!replaceCommandOnTab && !autocompleteOnTab)) return
            val autocompleteRow = list.selectedValue
            if (replaceCommandOnTab) {
                if (autocompleteRow is LiveCommandFieldRow && autocompleteRow.liveCommand.params.isNotEmpty()) {
                    val triggerPrefix = autocompleteRow.liveCommand.getTriggerName().lowercase() + " "
                    if (text.lowercase().startsWith(triggerPrefix)) {
                        return //do nothing
                    }
                    setText(autocompleteRow.getText() + " ")
                } else {
                    setText(autocompleteRow.getText())
                }
            } else {
                val userInput = text.substringAfterLast(" ")
                setText(text.substring(0, text.length - userInput.length) + autocompleteRow.getText())
            }
            caretPosition = text.length
        } else if (e.keyCode == KeyEvent.VK_ENTER) {
            if (!autocompleteAndFinishOnEnter) {
                ready = true
                actualText = text
                hideAutocompletePopup()
                return
            }
            actualText = text

            val text = if (isPopupVisible()) list.selectedValue else null
            if (text is LiveCommandFieldRow) {
                val liveCommand = text.liveCommand
                if (liveCommand.params.isNotEmpty()) {
                    if (!getText().lowercase().startsWith(liveCommand.getTriggerName().lowercase() + " ")) {
                        setText(text.getText() + " ")
                        caretPosition = getText().length
                    } else {
                        val params = substringAfterIgnoreCase(getText(), liveCommand.getTriggerName())
                            .split(" ").filter { it.isNotEmpty() }
                        if (params.size < liveCommand.params.size) {
                            setText(getText().trimEnd() + " ")
                            caretPosition = getText().length
                        } else {
                            ready = true
                        }
                    }
                } else {
                    ready = true
                }
            } else if (text != null) {
                addAutoCompleteToInput(text)
                ready = true
            }
        }
    }

    private fun scrollListToSelected() {
        val index = list.selectedIndex
        if (index != -1) {
            list.ensureIndexIsVisible(index)
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

            val textLength = g.fontMetrics.stringWidth(placeHolderText)
            val fieldLength = width

            g.color = placeHolderTextColor ?: PluginUI.getPlaceholderForeground()
            g.drawString(
                placeHolderText,
                insets.left + (fieldLength / 2) - (textLength / 2),
                g.fontMetrics.maxAscent + insets.top
            )
        }

        //paint live command argument hints
        if (list.selectedValue is LiveCommandFieldRow) {
            val liveCommand = (list.selectedValue as LiveCommandFieldRow).liveCommand
            if (!text.lowercase().startsWith(liveCommand.getTriggerName().lowercase())) return
            val params = substringAfterIgnoreCase(text, liveCommand.getTriggerName())
                .split(" ").filter { it.isNotEmpty() }

            var textOffset = 0
            for ((index, param) in liveCommand.params.withIndex()) {
                if (index < params.size) continue

                val paramTextX = insets.left + pG.getFontMetrics().stringWidth(text.trimEnd() + " ") + textOffset
                g.color = JBColor("PLACEHOLDER_FOREGROUND", Color(
                    UIUtil.getTextFieldForeground().red,
                    UIUtil.getTextFieldForeground().green,
                    UIUtil.getTextFieldForeground().blue,
                    75
                ))
                g.drawString("[$param]", paramTextX, pG.getFontMetrics().maxAscent + insets.top)
                textOffset += pG.getFontMetrics().stringWidth("[$param] ")
            }
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
