package com.sourceplusplus.sourcemarker.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import com.sourceplusplus.protocol.instrument.log.LiveLog
import com.sourceplusplus.sourcemarker.inlay.EditorComponentInlaysManager
import java.awt.BorderLayout
import java.awt.event.ComponentEvent
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object LiveLogStatusManager : SourceMarkEventListener {

    private val activeStatusBars = CopyOnWriteArrayList<LiveLog>()

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_ADDED -> {
                if (event.sourceMark !is MethodSourceMark) return
                ApplicationManager.getApplication().runReadAction {
                    val methodSourceMark = event.sourceMark as MethodSourceMark
                    val qualifiedClassName = methodSourceMark.sourceFileMarker.getClassQualifiedNames()[0]

                    val textRange = methodSourceMark.getPsiElement().textRange
                    val document = PsiDocumentManager.getInstance(methodSourceMark.project)
                        .getDocument(methodSourceMark.sourceFileMarker.psiFile)!!
                    val startLine = document.getLineNumber(textRange.startOffset) + 1
                    val endLine = document.getLineNumber(textRange.endOffset) + 1

                    activeStatusBars.forEach {
                        if (qualifiedClassName == it.location.source && it.location.line in startLine..endLine) {
                            showStatusBar(methodSourceMark.project, it, methodSourceMark)
                        }
                    }
                }
            }
        }
    }

    fun showStatusBar(editor: Editor, lineNumber: Int, focus: Boolean = true) {
        val project = editor.project!!
        val manager = EditorComponentInlaysManager.from(editor)
        val inlayRef = Ref<Inlay<*>>()

        val fileMarker = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)!!
        val qualifiedClassName = fileMarker.getClassQualifiedNames()[0]

        val wrapperPanel = JPanel()
        wrapperPanel.layout = BorderLayout()
        val statusBar = LogStatusBar(
            LiveSourceLocation(qualifiedClassName, lineNumber + 1),
            getMethodSourceMark(editor, fileMarker, lineNumber)
        )//test2.ActivityBar(editor as EditorImpl?, wrapperPanel)
        wrapperPanel.add(statusBar)
        statusBar.setInlayRef(inlayRef)

        val inlay = manager.insertAfter(lineNumber - 1, wrapperPanel)
        inlayRef.set(inlay)
        val viewport = (editor as? EditorImpl)?.scrollPane?.viewport
        viewport?.dispatchEvent(ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED))
        if (focus) {
            statusBar.focus()
        }
    }

    fun showStatusBar(project: Project, liveLog: LiveLog, methodSourceMark: MethodSourceMark) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
            val manager = EditorComponentInlaysManager.from(editor)
            val inlayRef = Ref<Inlay<*>>()

            val fileMarker = methodSourceMark.sourceFileMarker
            val qualifiedClassName = fileMarker.getClassQualifiedNames()[0]

            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()
            val statusBar = LogStatusBar(
                LiveSourceLocation(qualifiedClassName, liveLog.location.line),
                methodSourceMark, liveLog
            )
            wrapperPanel.add(statusBar)
            statusBar.setInlayRef(inlayRef)

            val inlay = manager.insertAfter(liveLog.location.line - 2, wrapperPanel)
            inlayRef.set(inlay)
            val viewport = (editor as? EditorImpl)?.scrollPane?.viewport
            viewport?.dispatchEvent(ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED))
        }
    }

    fun addActiveLiveLogs(liveLogs: List<LiveLog>) {
        activeStatusBars.addAll(liveLogs)
    }

    private fun getMethodSourceMark(editor: Editor, fileMarker: SourceFileMarker, line: Int): MethodSourceMark? {
        return fileMarker.getSourceMarks().find {
            if (it is MethodSourceMark) {
                if (it.configuration.activateOnKeyboardShortcut) {
                    //+1 on end offset so match is made even right after method end
                    val incTextRange = TextRange(
                        it.getPsiMethod().sourcePsi!!.textRange.startOffset,
                        it.getPsiMethod().sourcePsi!!.textRange.endOffset + 1
                    )
                    incTextRange.contains(editor.logicalPositionToOffset(LogicalPosition(line - 1, 0)))
                } else {
                    false
                }
            } else {
                false
            }
        } as MethodSourceMark?
    }
}
