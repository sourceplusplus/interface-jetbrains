package com.sourceplusplus.sourcemarker.status.util

import com.sourceplusplus.sourcemarker.command.AutocompleteFieldRow
import com.sourceplusplus.sourcemarker.command.CommandAction
import com.sourceplusplus.sourcemarker.element.AutocompleteRow
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class AutoCompleteCellRenderer(private val lineNumber: Int) : DefaultListCellRenderer() {
    init {
        isOpaque = true
    }

    override fun getListCellRendererComponent(
        list: JList<*>, value: Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val entry = value as AutocompleteFieldRow
        val row = AutocompleteRow()
        row.setCommandName(entry.getText())
        row.setCommandIcon(entry.getIcon())
        if (entry.getDescription() != null) {
            row.setDescription(
                entry.getDescription()!!.replace("*lineNumber*", (lineNumber - 1).toString())
            )
        }

        if (isSelected) {
            row.background = Color.decode("#1C1C1C")
            if (entry is CommandAction) {
                row.setCommandIcon(entry.selectedIcon)
            }
        }
        return row
    }
}
