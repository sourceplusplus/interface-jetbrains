package com.sourceplusplus.sourcemarker.status.util

import com.sourceplusplus.sourcemarker.command.AutocompleteFieldRow
import com.sourceplusplus.sourcemarker.command.LiveControlCommand
import com.sourceplusplus.sourcemarker.element.LiveControlBarRow
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ControlBarCellRenderer(private val autocompleteField: AutocompleteField) : DefaultListCellRenderer() {
    init {
        isOpaque = true
    }

    override fun getListCellRendererComponent(
        list: JList<*>, value: Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val entry = value as AutocompleteFieldRow
        val row = LiveControlBarRow()
        row.setCommandName(entry.getText(), autocompleteField.text)
        row.setCommandIcon(entry.getIcon())
        if (entry.getDescription() != null) {
            row.setDescription(
                entry.getDescription()!!.replace("*lineNumber*", autocompleteField.lineNumber.toString())
            )
        }

        if (isSelected) {
            row.background = Color.decode("#1C1C1C")
            if (entry is LiveControlCommand) {
                row.setCommandIcon(entry.selectedIcon)
            }
        }
        return row
    }
}
