/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.command.util

import com.intellij.codeInsight.daemon.impl.MainPassesRunner
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox
import spp.jetbrains.marker.SourceMarkerUtils.isJavaScript
import spp.jetbrains.marker.SourceMarkerUtils.isJvm
import spp.jetbrains.marker.SourceMarkerUtils.isPython
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

object ExpressionUtils {

    @JvmStatic
    fun checkForErrors(project: Project, document: Document): Boolean {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        val virtualFiles = ArrayList<VirtualFile>()
        virtualFiles.add(psiFile!!.virtualFile)

        //todo: should be more efficient way to force error check than MainPassesRunner
        val runner = MainPassesRunner(project, "Condition Check", null)
        val highlightInfos = runner.runMainPasses(virtualFiles)[document]
        if (highlightInfos != null) {
            for (highlightInfo in highlightInfos) {
                if (highlightInfo.severity === HighlightSeverity.ERROR) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun getExpressionComboBox(
        psiFile: PsiFile,
        lineNumber: Int,
        escapeDisposable: Disposable? = null,
        nextFocus: JComponent? = null,
        previousFocus: JComponent? = null
    ): XDebuggerExpressionComboBox {
        val sourcePosition = XDebuggerUtil.getInstance().createPosition(psiFile.virtualFile, lineNumber)
        val editorsProvider = if (isPython(psiFile.language)) {
            Class.forName(
                "com.jetbrains.python.debugger.PyDebuggerEditorsProvider"
            ).getDeclaredConstructor().newInstance() as XDebuggerEditorsProvider
        } else if (isJvm(psiFile.language)) {
            Class.forName(
                "org.jetbrains.java.debugger.JavaDebuggerEditorsProvider"
            ).getDeclaredConstructor().newInstance() as XDebuggerEditorsProvider
        } else if (isJavaScript(psiFile.language)) {
            Class.forName(
                "com.intellij.javascript.debugger.JSDebuggerEditorsProvider"
            ).getDeclaredConstructor().newInstance() as XDebuggerEditorsProvider
        } else {
            throw UnsupportedOperationException("Unsupported language: ${psiFile.language}")
        }
        val comboBox = XDebuggerExpressionComboBox(
            psiFile.project, editorsProvider, null, //todo: impl history
            sourcePosition, false, false
        )
        comboBox.comboBox.addActionListener {
            if (comboBox.comboBox.selectedItem != null) {
                comboBox.comboBox.selectedItem = null
            }
        }

        object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                escapeDisposable?.let { Disposer.dispose(it) }
            }
        }.registerCustomShortcutSet(
            CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),
            comboBox.editorComponent
        )
        object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val hasErrors = checkForErrors(psiFile.project, comboBox.editor!!.document)
                if (hasErrors) {
                    return //expression has error(s)
                }
                nextFocus?.let { IdeFocusManager.getInstance(psiFile.project).requestFocus(it, true) }
            }
        }.registerCustomShortcutSet(
            CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)),
            comboBox.editorComponent
        )
        object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val hasErrors = checkForErrors(psiFile.project, comboBox.editor!!.document)
                if (hasErrors) {
                    return //expression has error(s)
                }
                nextFocus?.let { IdeFocusManager.getInstance(psiFile.project).requestFocus(it, true) }
            }
        }.registerCustomShortcutSet(
            CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
            comboBox.editorComponent
        )
        object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                previousFocus?.let { IdeFocusManager.getInstance(psiFile.project).requestFocus(it, true) }
            }
        }.registerCustomShortcutSet(
            CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)),
            comboBox.editorComponent
        )
        comboBox.editorComponent.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)

        return comboBox
    }
}
