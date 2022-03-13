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
package spp.jetbrains.sourcemarker.service.instrument.breakpoint.painter

import com.intellij.openapi.editor.EditorLinePainter
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.xdebugger.ui.DebuggerColors
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.BreakpointHitWindowService
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
                    TextAttributes(JBColor {
                        if (EditorColorsManager.getInstance().isDarkEditor
                        ) Color(0x3d8065) else Gray._135
                    }, null, null, null, Font.ITALIC)
                } else attributes
            }
    }
}
