/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.plugin

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.invokeLater
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.gutter.GutterMark
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
        if (DumbService.isDumb(source.project) || !file.isValid) {
            log.debug("Ignoring file closed: $file")
        } else {
            log.debug("File closed: $file")
            val psiFile = PsiManager.getInstance(source.project).findFile(file)
            val fileMarker = SourceFileMarker.getIfExists(psiFile)
            if (fileMarker != null) {
                safeRunBlocking {
                    SourceMarker.getInstance(source.project).deactivateSourceFileMarker(fileMarker)
                }
            }
        }
    }

    companion object {
        private val log = logger<FileActivityListener>()

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
                log.warn("Selected editor was null. Failed to add mouse motion listener")
            }
        }

        //todo: belongs closer to gutter mark code
        private fun makeMouseMotionListener(editor: Editor, psiFile: PsiFile): EditorMouseMotionListener {
            return object : EditorMouseMotionListener {
                override fun mouseMoved(e: EditorMouseEvent) {
                    if (e.area != EditorMouseEventArea.LINE_MARKERS_AREA || e.isConsumed) {
                        return
                    }

                    val lineNumber = convertPointToLineNumber(editor, e.mouseEvent.point)
                    if (lineNumber == -1) {
                        return
                    }

                    var syncViewProvider = false
                    var gutterMark: GutterMark? = null
                    val fileMarker = SourceFileMarker.getOrCreate(psiFile)
                    if (fileMarker != null) {
                        gutterMark = fileMarker.getSourceMarks().find {
                            if (it is GutterMark) {
                                if (it.configuration.activateOnMouseHover && it.configuration.icon != null) {
                                    if (it.viewProviderBound) {
                                        it.lineNumber == lineNumber + 1
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
                            psiFile.project.invokeLater {
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

        private fun convertPointToLineNumber(editor: Editor, p: Point): Int {
            val document = editor.document
            val line = EditorUtil.yPositionToLogicalLine(editor, p)
            if (!isValidLine(document, line)) return -1
            val startOffset = document.getLineStartOffset(line)
            val region = editor.foldingModel.getCollapsedRegionAtOffset(startOffset)
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
