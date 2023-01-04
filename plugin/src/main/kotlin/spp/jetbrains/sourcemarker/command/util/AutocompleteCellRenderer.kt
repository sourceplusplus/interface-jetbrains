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

import spp.jetbrains.PluginUI.getBackgroundFocusColor
import spp.jetbrains.PluginUI.getBackgroundUnfocusedColor
import spp.jetbrains.sourcemarker.command.status.ui.element.AutocompleteRow
import spp.protocol.artifact.ArtifactNameUtils.getShortFunctionSignature
import spp.protocol.artifact.ArtifactQualifiedName
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class AutocompleteCellRenderer(private val artifactQualifiedName: ArtifactQualifiedName) : DefaultListCellRenderer() {
    init {
        isOpaque = true
    }

    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val entry = value as AutocompleteFieldRow
        val row = AutocompleteRow()
        row.setCommandName(entry.getText())
        row.setCommandIcon(entry.getUnselectedIcon())
        if (entry.getDescription() != null) {
            row.setDescription(
                entry.getDescription()!!
                    .replace("*lineNumber*", artifactQualifiedName.lineNumber.toString())
                    .replace("*methodName*", getShortFunctionSignature(artifactQualifiedName.identifier))
            )
        }

        if (isSelected) {
            row.background = getBackgroundFocusColor()
            row.setCommandIcon(entry.getSelectedIcon())
        } else {
            row.background = getBackgroundUnfocusedColor()
        }
        return row
    }
}
