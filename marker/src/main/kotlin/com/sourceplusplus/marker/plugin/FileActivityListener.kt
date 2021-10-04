package com.sourceplusplus.marker.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import org.slf4j.LoggerFactory
import java.awt.Point

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class FileActivityListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (DumbService.isDumb(source.project)) {
            log.debug("Ignoring file opened: $file")
        } else {
            triggerFileOpened(source, file)
        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        if (DumbService.isDumb(source.project)) {
            log.debug("Ignoring file closed: $file")
        } else {
            log.debug("File closed: $file")
            val psiFile = PsiManager.getInstance(source.project).findFile(file)!!
            val fileMarker = psiFile.getUserData(SourceFileMarker.KEY)
            if (fileMarker != null) {
                SourceMarker.deactivateSourceFileMarker(fileMarker)
            }
        }
    }

    companion object {

        private val log = LoggerFactory.getLogger(FileActivityListener::class.java)

        @JvmStatic
        fun triggerFileOpened(source: FileEditorManager, file: VirtualFile) {
            log.debug("File opened: $file")

            //display gutter mark popups on hover over gutter mark
            val editor = EditorUtil.getEditorEx(source.getSelectedEditor(file))
            if (editor != null) {
                val psiFile = PsiManager.getInstance(source.project).findFile(file)!!
                val editorMouseMotionListener = makeMouseMotionListener(editor, psiFile)
                editor.addEditorMouseMotionListener(editorMouseMotionListener)
            } else {
                log.error("Selected editor was null. Failed to add mouse motion listener")
            }
        }

        //todo: belongs closer to gutter mark code
        private fun makeMouseMotionListener(editor: Editor, psiFile: PsiFile): EditorMouseMotionListener {
            return object : EditorMouseMotionListener {
                override fun mouseMoved(e: EditorMouseEvent) {
                    if (e.area != EditorMouseEventArea.LINE_MARKERS_AREA || e.isConsumed) {
                        return
                    }

                    val lineNumber = convertPointToLineNumber(psiFile.project, e.mouseEvent.point)
                    if (lineNumber == -1) {
                        return
                    }

                    var syncViewProvider = false
                    var gutterMark: GutterMark? = null
                    val fileMarker = psiFile.getUserData(SourceFileMarker.KEY)
                    if (fileMarker != null) {
                        gutterMark = fileMarker.getSourceMarks().find {
                            if (it is GutterMark) {
                                if (it.configuration.activateOnMouseHover && it.configuration.icon != null) {
                                    if (it.viewProviderBound) {
                                        it.lineNumber == lineNumber
                                    } else {
                                        syncViewProvider = true
                                        false
                                    }
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        } as GutterMark?

                        if (syncViewProvider) {
                            //todo: better fix (prevents #8)
                            ApplicationManager.getApplication().invokeLater {
                                val fileEditorManager = FileEditorManager.getInstance(fileMarker.project)
                                fileEditorManager.closeFile(fileMarker.psiFile.virtualFile)
                                fileEditorManager.openFile(fileMarker.psiFile.virtualFile, true)
                            }
                        }
                    }

                    if (gutterMark != null) {
                        e.consume()
                        gutterMark.displayPopup(editor)
                    }
                }
            }
        }

        private fun convertPointToLineNumber(project: Project, p: Point): Int {
            val myEditor = FileEditorManager.getInstance(project).selectedTextEditor
            val document = myEditor!!.document
            val line = EditorUtil.yPositionToLogicalLine(myEditor, p)
            if (!isValidLine(document, line)) return -1
            val startOffset = document.getLineStartOffset(line)
            val region = myEditor.foldingModel.getCollapsedRegionAtOffset(startOffset)
            return if (region != null) {
                document.getLineNumber(region.endOffset)
            } else line
        }

        private fun isValidLine(document: Document, line: Int): Boolean {
            if (line < 0) return false
            val lineCount = document.lineCount
            return if (lineCount == 0) line == 0 else line < lineCount
        }
    }
}
