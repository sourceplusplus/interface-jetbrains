package spp.jetbrains.sourcemarker.status.util

import spp.jetbrains.sourcemarker.PluginUI.BGND_FOCUS_COLOR
import spp.jetbrains.sourcemarker.command.AutocompleteFieldRow
import spp.jetbrains.sourcemarker.command.LiveControlCommand
import spp.jetbrains.sourcemarker.element.AutocompleteRow
import spp.protocol.artifact.ArtifactQualifiedName
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
class AutoCompleteCellRenderer(private val artifactQualifiedName: ArtifactQualifiedName) : DefaultListCellRenderer() {
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
                entry.getDescription()!!
                    .replace("*lineNumber*", artifactQualifiedName.lineNumber.toString())
                    .replace("*methodName*", getShortFunctionSignature(artifactQualifiedName.identifier))
            )
        }

        if (isSelected) {
            row.background = BGND_FOCUS_COLOR;
            if (entry is LiveControlCommand) {
                row.setCommandIcon(entry.selectedIcon)
            }
        }
        return row
    }
}
