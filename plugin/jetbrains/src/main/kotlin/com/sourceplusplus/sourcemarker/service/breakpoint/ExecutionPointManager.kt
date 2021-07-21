package com.sourceplusplus.sourcemarker.service.breakpoint

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.ClassUtil
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import com.sourceplusplus.protocol.artifact.exception.qualifiedClassName
import com.sourceplusplus.protocol.artifact.exception.sourceAsLineNumber
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExecutionPointManager(
    private val project: Project,
    private val executionPointHighlighter: ExecutionPointHighlighter
) : DebugStackFrameListener, Disposable {

    companion object {
        private val log = LoggerFactory.getLogger(ExecutionPointManager::class.java)
    }

    override fun onChanged(stackFrameManager: StackFrameManager) {
        val currentFrame = stackFrameManager.currentFrame
        var fromClass = currentFrame!!.qualifiedClassName()

        //check for inner class
        val indexOfDollarSign = fromClass.indexOf("$")
        if (indexOfDollarSign >= 0) {
            fromClass = fromClass.substring(0, indexOfDollarSign)
        }
        val containingClass = ClassUtil.findPsiClass(
            PsiManager.getInstance(
                project
            ), fromClass
        ) ?: return
        val virtualFile = containingClass.containingFile.virtualFile ?: return
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return
        val lineStartOffset = document.getLineStartOffset(currentFrame.sourceAsLineNumber()!!) - 1

        ApplicationManager.getApplication().invokeLater {
            try {
                FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptor(project, virtualFile, lineStartOffset), true
                )
                executionPointHighlighter.hide()
                executionPointHighlighter.show(
                    XDebuggerUtil.getInstance().createPositionByOffset(
                        virtualFile, lineStartOffset
                    )!!, false, null
                )
            } catch (e: Throwable) {
                log.error("Failed to set execution point", e)
            }
        }
    }

    override fun dispose() {
        executionPointHighlighter.hide()
    }
}
