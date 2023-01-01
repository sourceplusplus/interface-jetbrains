/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.sourcemarker.instrument.breakpoint.painter

import com.intellij.openapi.editor.EditorLinePainter
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.xdebugger.ui.DebuggerColors
import spp.jetbrains.sourcemarker.instrument.breakpoint.BreakpointHitWindowService
import java.awt.Color
import java.awt.Font

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class VariableEditorLinePainter : EditorLinePainter() {

    override fun getLineExtensions(
        project: Project,
        file: VirtualFile,
        lineNumber: Int
    ): Collection<LineExtensionInfo> {
        val lineInfos: MutableList<LineExtensionInfo> = ArrayList()
        val bpWindow = BreakpointHitWindowService.getInstance(project).getBreakpointWindow()
        if (bpWindow?.stackFrameManager?.currentFrame != null) {
            val vars = bpWindow.stackFrameManager.currentFrame!!.variables
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
                val attributes =
                    EditorColorsManager.getInstance().globalScheme.getAttributes(DebuggerColors.INLINED_VALUES)
                return if (attributes == null || attributes.foregroundColor == null) {
                    TextAttributes(JBColor.lazy {
                        if (EditorColorsManager.getInstance().isDarkEditor
                        ) Color(0x3d8065) else Gray._135
                    }, null, null, null, Font.ITALIC)
                } else attributes
            }
    }
}
