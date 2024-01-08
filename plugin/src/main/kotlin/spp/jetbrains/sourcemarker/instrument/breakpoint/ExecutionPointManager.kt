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
package spp.jetbrains.sourcemarker.instrument.breakpoint

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.invokeLater
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.sourcemarker.instrument.breakpoint.model.ActiveStackTrace
import spp.jetbrains.sourcemarker.instrument.breakpoint.model.ActiveStackTraceListener
import spp.protocol.artifact.exception.sourceAsLineNumber

/**
 * Shows the execution point in the editor for the selected stack frame.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExecutionPointManager(
    private val project: Project,
    private val executionPointHighlighter: ExecutionPointHighlighter
) : ActiveStackTraceListener, Disposable {

    companion object {
        private val log = logger<ExecutionPointManager>()
    }

    override fun onChanged(activeStackTrace: ActiveStackTrace) {
        val currentFrame = activeStackTrace.currentFrame ?: return
        val psiFile = activeStackTrace.stackTrace.language?.let {
            ArtifactNamingService.getService(it).findPsiFile(it, project, currentFrame)
        } ?: return
        val virtualFile = ArtifactScopeService.findSourceFile(psiFile) ?: return
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return
        val lineStartOffset = currentFrame.sourceAsLineNumber()?.let {
            document.getLineStartOffset(it) - 1
        } ?: return
        project.invokeLater {
            try {
                FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptor(project, virtualFile, lineStartOffset), false
                )
                executionPointHighlighter.hide()
                executionPointHighlighter.show(
                    XDebuggerUtil.getInstance().createPositionByOffset(
                        virtualFile, lineStartOffset
                    )!!, true, null
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
