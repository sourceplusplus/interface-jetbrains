package spp.jetbrains.sourcemarker.service.breakpoint

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import spp.jetbrains.marker.SourceMarker
import spp.protocol.artifact.exception.qualifiedClassName
import spp.protocol.artifact.exception.sourceAsLineNumber
import org.slf4j.LoggerFactory

/**
 * todo: probably don't need this as the breakpoint bar serves as the execution point indicator
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExecutionPointManager(
    private val project: Project,
    private val executionPointHighlighter: ExecutionPointHighlighter,
    private val showExecutionPoint: Boolean = true
) : DebugStackFrameListener, Disposable {

    companion object {
        private val log = LoggerFactory.getLogger(ExecutionPointManager::class.java)
    }

    override fun onChanged(stackFrameManager: StackFrameManager) {
        if (!showExecutionPoint) return
        val currentFrame = stackFrameManager.currentFrame
        var fromClass = currentFrame!!.qualifiedClassName()

        //check for inner class
        val indexOfDollarSign = fromClass.indexOf("$")
        if (indexOfDollarSign >= 0) {
            fromClass = fromClass.substring(0, indexOfDollarSign)
        }
        val fileMarker = SourceMarker.getSourceFileMarker(fromClass) ?: return
        val virtualFile = fileMarker.psiFile.containingFile.virtualFile ?: return
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
