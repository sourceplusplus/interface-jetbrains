/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.status.util

import spp.jetbrains.PluginUI.BGND_FOCUS_COLOR
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
class ControlBarCellRenderer(
    private val autocompleteField: AutocompleteField<LiveCommandFieldRow>
) : DefaultListCellRenderer() {
    init {
        isOpaque = false
    }

    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val rowValue = value as LiveCommandFieldRow
        val entry = rowValue.liveCommand

        val row = LiveControlBarRow()
        row.setCommandName(entry.name, autocompleteField.text)
        row.setCommandIcon(rowValue.getUnselectedIcon())

        var formattedDescription = entry.description
        if (formattedDescription.contains("*lineNumber*")) {
            formattedDescription = formattedDescription.replace(
                "*lineNumber*",
                autocompleteField.artifactQualifiedName.lineNumber.toString()
            )
        }
        if (formattedDescription.contains("*lineNumber*")) {
            formattedDescription = formattedDescription.replace(
                "*methodName*",
                getShortFunctionSignature(autocompleteField.artifactQualifiedName.identifier)
            )
        }
        row.setDescription(formattedDescription)

        if (isSelected) {
            row.background = BGND_FOCUS_COLOR
            row.setCommandIcon(rowValue.getSelectedIcon())
        }
        return row
    }
}
