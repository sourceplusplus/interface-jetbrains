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
package spp.jetbrains.sourcemarker.command

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.annotations.NotNull

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ControlBarAction : AnAction() {

    override fun update(@NotNull e: AnActionEvent) {
        val project: Project? = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val lineNumber = editor.document.getLineNumber(editor.caretModel.offset)
        val lineStart = editor.document.getLineStartOffset(lineNumber)
        val lineEnd = editor.document.getLineEndOffset(lineNumber)
        val text = editor.document.getText(TextRange(lineStart, lineEnd))
        val codeStartsAt = text.length - text.trimStart().length
        if (editor.caretModel.offset <= lineStart + codeStartsAt) {
            //caret is before the current line's code
            ControlBarController.showControlBar(editor, lineNumber)
        } else {
            ControlBarController.showControlBar(editor, lineNumber + 1)
        }
    }
}
