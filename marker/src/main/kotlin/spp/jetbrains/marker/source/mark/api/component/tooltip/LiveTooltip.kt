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
package spp.jetbrains.marker.source.mark.api.component.tooltip

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.awt.RelativePoint
import spp.jetbrains.invokeLater
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.guide.GuideMark
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel

open class LiveTooltip(val guideMark: GuideMark, var panel: JPanel? = null) {

    init {
        guideMark.project.invokeLater {
            install(guideMark.project)
        }
    }

    companion object {
        private val log = logger<LiveTooltip>()
        var updateAnnotator = false
        val holdingShift = AtomicBoolean(false)
        private val showingLiveTooltip = AtomicReference<LiveTooltip>()
        private val installedListeners = AtomicBoolean(false)

        fun install(project: Project) {
            if (installedListeners.compareAndSet(true, true)) return

            var mousePosition: VisualPosition? = null
            FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
                val keyListener: KeyListener = object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        super.keyPressed(e)
                        if (e.keyCode == KeyEvent.VK_SHIFT) {
                            holdingShift.set(true)
                        }
                        if (holdingShift.get() && updateAnnotator) {
                            DaemonCodeAnalyzer.getInstance(project).restart()
                        }

                        val liveDisplay = getLiveDisplay(editor, mousePosition) ?: return
                        if (holdingShift.get() && showingLiveTooltip.get() == null) {
                            mousePosition?.let { tryShowDisplay(editor, liveDisplay) }
                        }
                    }

                    override fun keyReleased(e: KeyEvent) {
                        super.keyReleased(e)
                        if (e.keyCode == KeyEvent.VK_SHIFT) {
                            holdingShift.set(false)
                        }
                        if (holdingShift.get() && updateAnnotator) {
                            DaemonCodeAnalyzer.getInstance(project).restart()
                        }

                        val liveDisplay = getLiveDisplay(editor, mousePosition) ?: return
                        if (holdingShift.get() && showingLiveTooltip.get() != null) {
                            if (liveDisplay.panel!!.mousePosition != null) return
                            HintManager.getInstance().hideAllHints()
                        }
                    }

                    override fun keyTyped(e: KeyEvent) {
                        super.keyTyped(e)
                        if (e.keyCode == KeyEvent.VK_SHIFT) {
                            holdingShift.set(false)
                        }
                        if (holdingShift.get() && updateAnnotator) {
                            DaemonCodeAnalyzer.getInstance(project).restart()
                        }

                        val liveDisplay = getLiveDisplay(editor, mousePosition) ?: return
                        if (holdingShift.get() && showingLiveTooltip.get() == null) {
                            if (liveDisplay.panel!!.mousePosition != null) return
                            HintManager.getInstance().hideAllHints()
                        }
                    }
                }
                editor.contentComponent.addKeyListener(keyListener)

                editor.addEditorMouseMotionListener(object : EditorMouseMotionListener {
                    override fun mouseMoved(e: EditorMouseEvent) {
                        super.mouseMoved(e)
                        mousePosition = e.visualPosition
                        if (showingLiveTooltip.get() != null && !e.mouseEvent.isShiftDown) {
                            HintManager.getInstance().hideAllHints()
                        }

                        val liveDisplay = getLiveDisplay(e.editor, mousePosition) ?: return
                        if (showingLiveTooltip.get() != liveDisplay && e.mouseEvent.isShiftDown) {
                            HintManager.getInstance().hideAllHints()
                            tryShowDisplay(e.editor, liveDisplay)
                        }
                    }
                })

                editor.scrollingModel.addVisibleAreaListener {
                    if (showingLiveTooltip.get() == null) return@addVisibleAreaListener
                    HintManager.getInstance().hideAllHints()
                }
            }
        }

        private fun getLiveDisplays(editor: Editor): List<LiveTooltip> {
            val project = editor.project ?: return emptyList()
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return emptyList()
            return SourceMarker.getSourceFileMarker(psiFile)
                ?.getSourceMarks()?.filterIsInstance<GuideMark>()?.mapNotNull { it.configuration.liveTooltip }
                ?: emptyList()
        }

        fun getLiveDisplay(editor: Editor, position: VisualPosition?): LiveTooltip? {
            if (position == null) return null
            return getLiveDisplays(editor).firstOrNull {
                val startPosition = editor.offsetToVisualPosition(it.guideMark.getPsiElement().startOffset)
                val endPosition = editor.offsetToVisualPosition(it.guideMark.getPsiElement().endOffset)
                position.after(startPosition) && !position.after(endPosition)
            }
        }

        fun tryShowDisplay(editor: Editor, liveTooltip: LiveTooltip) {
            val point = editor.offsetToXY(liveTooltip.guideMark.getPsiElement().startOffset)
            point.y -= liveTooltip.panel!!.preferredSize.height
            val relativePoint = RelativePoint(editor.contentComponent, point)

            if (showingLiveTooltip.compareAndSet(null, liveTooltip)) {
                log.debug("Showing live display. Guide mark: ${liveTooltip.guideMark}")
                //HintManager.getInstance().setRequestFocusForNextHint(true)
                HintManager.getInstance().showHint(
                    liveTooltip.panel!!,
                    relativePoint,
                    HintManager.HIDE_BY_ANY_KEY or
                            HintManager.HIDE_BY_TEXT_CHANGE or
                            HintManager.HIDE_BY_SCROLLING or
                            HintManager.HIDE_BY_OTHER_HINT,
                    0,
                ) {
                    showingLiveTooltip.set(null)
                }
            }
        }
    }
}
