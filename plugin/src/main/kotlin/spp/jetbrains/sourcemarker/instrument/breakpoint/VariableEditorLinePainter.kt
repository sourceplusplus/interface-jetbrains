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
package spp.jetbrains.sourcemarker.instrument.breakpoint

import com.intellij.openapi.editor.EditorLinePainter
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.xdebugger.ui.DebuggerColors
import spp.jetbrains.sourcemarker.instrument.InstrumentEventWindowService
import spp.jetbrains.sourcemarker.instrument.breakpoint.model.ActiveStackTrace
import spp.protocol.instrument.variable.LiveVariable
import java.awt.Color
import java.awt.Font

/**
 * Displays [LiveVariable] values directly in the editor for the [ActiveStackTrace.currentFrame].
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableEditorLinePainter : EditorLinePainter() {

    override fun getLineExtensions(project: Project, file: VirtualFile, lineNumber: Int): List<LineExtensionInfo> {
        val lineInfos = mutableListOf<LineExtensionInfo>()
        val bpHitTab = InstrumentEventWindowService.getInstance(project).getBreakpointHitTab()
        if (bpHitTab?.activeStack?.currentFrame != null) {
            val vars = bpHitTab.activeStack.currentFrame!!.variables
            val attributes = normalAttributes
            vars.forEach {
                if (it.lineNumber == lineNumber + 1) {
                    lineInfos.add(LineExtensionInfo("  ${it.name} = ${it.value}", attributes))
                }
            }
        }
        return lineInfos
    }

    companion object {
        private val normalAttributes: TextAttributes
            get() {
                val colorsManager = EditorColorsManager.getInstance()
                val attributes = colorsManager.globalScheme.getAttributes(DebuggerColors.INLINED_VALUES)
                return if (attributes == null || attributes.foregroundColor == null) {
                    TextAttributes(JBColor.lazy {
                        if (colorsManager.isDarkEditor) Color(0x3d8065) else Gray._135
                    }, null, null, null, Font.ITALIC)
                } else attributes
            }
    }
}
