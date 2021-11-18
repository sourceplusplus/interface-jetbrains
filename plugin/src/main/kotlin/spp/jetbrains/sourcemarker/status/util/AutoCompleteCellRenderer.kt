package spp.jetbrains.sourcemarker.status.util

import spp.jetbrains.sourcemarker.command.AutocompleteFieldRow
import spp.jetbrains.sourcemarker.command.LiveControlCommand
import spp.jetbrains.sourcemarker.element.AutocompleteRow
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
                entry.getDescription()!!.replace("*lineNumber*", lineNumber.toString())
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
