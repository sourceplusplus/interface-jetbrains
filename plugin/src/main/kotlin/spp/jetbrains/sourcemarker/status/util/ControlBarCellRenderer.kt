/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.status.util

import spp.jetbrains.sourcemarker.PluginUI
import spp.jetbrains.sourcemarker.command.AutocompleteFieldRow
import spp.jetbrains.sourcemarker.command.LiveControlCommand
import spp.jetbrains.sourcemarker.element.LiveControlBarRow
import spp.protocol.artifact.ArtifactNameUtils.getShortFunctionSignature
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
