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
package spp.jetbrains.sourcemarker.command

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.marker.SourceMarkerUtils.substringAfterIgnoreCase
import spp.jetbrains.marker.command.LiveCommand
import spp.jetbrains.marker.command.LiveCommandContext
import spp.jetbrains.marker.command.LiveLocationContext
import spp.jetbrains.marker.plugin.LivePluginService
import spp.jetbrains.marker.service.ArtifactCreationService
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.guide.ClassGuideMark
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.sourcemarker.command.ui.ControlBar
import spp.jetbrains.status.SourceStatusService
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object CommandBarController {

    private val log = logger<CommandBarController>()
    private var previousControlBar: InlayMark? = null

    private fun determineAvailableCommandsAtLocation(inlayMark: ExpressionInlayMark): List<LiveCommand> {
        val availableCommandsAtLocation = mutableSetOf<LiveCommand>()
        availableCommandsAtLocation.addAll(
            LivePluginService.getInstance(inlayMark.project).getRegisteredLiveCommands(
                LiveLocationContext(
                    inlayMark.artifactQualifiedName,
                    inlayMark.sourceFileMarker,
                    inlayMark.getPsiElement()
                )
            )
        )
        return availableCommandsAtLocation.toList()
    }

    @JvmStatic
    fun handleCommandInput(input: String, editor: Editor) {
        handleCommandInput(input, input, editor)
    }

    @JvmStatic
    fun handleCommandInput(input: String, fullText: String, editor: Editor) {
        log.info("Processing command input: $input")
        LivePluginService.getInstance(editor.project!!).getRegisteredLiveCommands()
            .find { it.getTriggerName() == input }?.let {
                val prevCommandBar = previousControlBar!!
                safeRunBlocking {
                    previousControlBar!!.disposeSuspend()
                    previousControlBar = null
                }

                val argsString = substringAfterIgnoreCase(fullText, input).trim()
                val args = if (argsString.isEmpty()) emptyList() else argsString.split(" ")
                val variableName = ArtifactNamingService.getVariableName(prevCommandBar.getPsiElement())

                val guideMark = getClosestGuideMark(prevCommandBar.sourceFileMarker, editor)
                it.trigger(
                    LiveCommandContext(
                        args,
                        prevCommandBar.sourceFileMarker.psiFile.virtualFile.toNioPath().toFile(),
                        prevCommandBar.lineNumber,
                        prevCommandBar.artifactQualifiedName,
                        prevCommandBar.sourceFileMarker,
                        guideMark,
                        prevCommandBar.getPsiElement(),
                        variableName
                    )
                )
            }
    }

    /**
     * Attempts to display live control bar below [lineNumber].
     */
    @JvmStatic
    fun showCommandBar(editor: Editor, lineNumber: Int) {
        //close previous command bar (if open)
        previousControlBar?.dispose(true, false)
        previousControlBar = null

        //ensure logged in
        if (!SourceStatusService.getInstance(editor.project!!).isLoggedIn()) {
            return
        }

        //determine control bar location
        val fileMarker = SourceFileMarker.getOrCreate(editor) ?: return
        val findInlayMark = ArtifactCreationService.getOrCreateExpressionInlayMark(fileMarker, lineNumber)
        if (findInlayMark.isPresent && ArtifactScopeService.canShowControlBar(findInlayMark.get().getPsiElement())) {
            val inlayMark = findInlayMark.get()
            if (!fileMarker.containsSourceMark(inlayMark)) {
                //create and display control bar
                previousControlBar = inlayMark

                val wrapperPanel = JPanel()
                wrapperPanel.layout = BorderLayout()

                val controlBar = ControlBar(editor, inlayMark, determineAvailableCommandsAtLocation(inlayMark))
                wrapperPanel.add(controlBar)
                editor.scrollingModel.addVisibleAreaListener(controlBar)

                inlayMark.configuration.showComponentInlay = true
                inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                    override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                }
                inlayMark.visible.set(true)
                inlayMark.apply()

                controlBar.focus()
            }
        }
    }

    private fun canShowCommandBar(fileMarker: SourceFileMarker, lineNumber: Int): InlayMark? {
        val inlayMark = ArtifactCreationService.getOrCreateExpressionInlayMark(fileMarker, lineNumber)
        return if (inlayMark.isPresent && ArtifactScopeService.canShowControlBar(inlayMark.get().getPsiElement())) {
            inlayMark.get()
        } else null
    }

    @JvmStatic
    fun getNextAvailableCommandBarLine(editor: Editor, inlayMark: InlayMark, moveUp: Boolean): Int? {
        var lineNumber = inlayMark.artifactQualifiedName.lineNumber!!
        if (moveUp) {
            while (--lineNumber > 0) {
                val nextMark = canShowCommandBar(inlayMark.sourceFileMarker, lineNumber)
                if (nextMark != null && nextMark.artifactQualifiedName != inlayMark.artifactQualifiedName) {
                    return lineNumber
                }
            }
        } else {
            while (++lineNumber < editor.document.lineCount) {
                val nextMark = canShowCommandBar(inlayMark.sourceFileMarker, lineNumber)
                if (nextMark != null && nextMark.artifactQualifiedName != inlayMark.artifactQualifiedName) {
                    return lineNumber
                }
            }
        }
        return null
    }

    private fun getClosestGuideMark(sourceFileMarker: SourceFileMarker, editor: Editor): GuideMark? {
        var classSourceMark: ClassGuideMark? = null
        val sourceMark = sourceFileMarker.getSourceMarks().filterIsInstance<GuideMark>().find {
            if (it is ClassGuideMark) {
                classSourceMark = it //todo: probably doesn't handle inner classes well
                false
            } else if (it is MethodGuideMark) {
                if (it.configuration.activateOnKeyboardShortcut) {
                    //+1 on end offset so match is made even right after method end
                    val incTextRange = TextRange(
                        it.getPsiMethod().textRange.startOffset,
                        it.getPsiMethod().textRange.endOffset + 1
                    )
                    incTextRange.contains(editor.logicalPositionToOffset(editor.caretModel.logicalPosition))
                } else {
                    false
                }
            } else {
                false
            }
        }
        return sourceMark ?: classSourceMark
    }
}
