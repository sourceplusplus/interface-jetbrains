package com.sourceplusplus.sourcemarker.command

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class CommandBarAction : AnAction() {

    override fun update(@NotNull e: AnActionEvent) {
        val project: Project? = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val lineNumber = editor.document.getLineNumber(editor.caretModel.offset) + 2
        CommandBarController.showCommandBar(editor, lineNumber)
    }
}
