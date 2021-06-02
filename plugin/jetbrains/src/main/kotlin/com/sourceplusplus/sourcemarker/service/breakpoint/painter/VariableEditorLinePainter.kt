package com.sourceplusplus.sourcemarker.service.breakpoint.painter

import com.intellij.openapi.editor.EditorLinePainter
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.xdebugger.ui.DebuggerColors
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointHitWindowService
import java.awt.Color
import java.awt.Font
import java.util.*

/**
 * todo: description.
 *
 * @since 0.2.2
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
