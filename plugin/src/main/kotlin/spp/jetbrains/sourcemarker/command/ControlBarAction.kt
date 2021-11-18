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
