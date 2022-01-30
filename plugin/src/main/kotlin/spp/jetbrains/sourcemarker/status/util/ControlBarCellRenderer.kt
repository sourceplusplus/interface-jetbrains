package spp.jetbrains.sourcemarker.status.util

import spp.jetbrains.sourcemarker.PluginUI
import spp.jetbrains.sourcemarker.command.AutocompleteFieldRow
import spp.jetbrains.sourcemarker.command.LiveControlCommand
import spp.jetbrains.sourcemarker.element.LiveControlBarRow
import spp.protocol.utils.ArtifactNameUtils.getShortFunctionSignature
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
        isOpaque = false
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
                entry.getDescription()!!
                    .replace(
                        "*lineNumber*",
                        autocompleteField.artifactQualifiedName.lineNumber.toString()
                    )
                    .replace(
                        "*methodName*",
                        getShortFunctionSignature(autocompleteField.artifactQualifiedName.identifier)
                    )
            )
        }

        if (isSelected) {
            row.background = PluginUI.AUTO_COMPLETE_HIGHLIGHT_COLOR
            if (entry is LiveControlCommand) {
                row.setCommandIcon(entry.selectedIcon)
            }
        }
        return row
    }
}
