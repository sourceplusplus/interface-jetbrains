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
package spp.jetbrains.marker.source.mark.api.component.tooltip

import com.intellij.util.ui.UIUtil
import spp.jetbrains.PluginUI
import spp.jetbrains.marker.source.mark.guide.GuideMark
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder

class TextLiveTooltip : LiveTooltip {

    private val label: JTextField
    var text: String
        get() = label.text
        set(value) {
            label.text = value
        }

    constructor(guideMark: GuideMark, text: String) : super(guideMark) {
        this.text = text
        panel = JPanel(BorderLayout())
        label = JTextField(text)
        label.isEditable = false
        panel!!.background = PluginUI.getInputBackgroundColor()
        label.background = PluginUI.getInputBackgroundColor()
        label.isOpaque = false
        label.border = null
        panel!!.border = CompoundBorder(
            LineBorder(UIUtil.getBoundsColor(), 0, true),
            EmptyBorder(3, 3, 3, 3)
        )
        panel!!.add(label, BorderLayout.CENTER)
    }
}
