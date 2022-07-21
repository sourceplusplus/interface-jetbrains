/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.command

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLocalVariable
import kotlinx.coroutines.runBlocking
import liveplugin.implementation.common.toFilePath
import liveplugin.implementation.plugin.LivePluginService
import org.joor.Reflect
import org.slf4j.LoggerFactory
import spp.command.LiveCommand
import spp.command.LiveCommandContext
import spp.jetbrains.marker.impl.ArtifactCreationService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.sourcemarker.ControlBar
import spp.jetbrains.sourcemarker.mark.SourceMarkSearch
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ControlBarController {

    private val log = LoggerFactory.getLogger(ControlBarController::class.java)
    private var previousControlBar: InlayMark? = null
    private val availableCommands: MutableList<LiveCommand> = mutableListOf()

    fun clearAvailableCommands() {
        availableCommands.clear()
    }

    private suspend fun syncAvailableCommands() {
        availableCommands.clear()

//        val selfInfo = SourceServices.Instance.liveService!!.getSelf().await()
//        availableCommands.addAll(LiveControlCommand.values().toList().filter {
//            @Suppress("UselessCallOnCollection") //unknown enums are null
//            selfInfo.permissions.filterNotNull().map { it.name }.contains(it.name)
//        })
    }

    private fun determineAvailableCommandsAtLocation(inlayMark: ExpressionInlayMark): List<LiveCommand> {
        if (availableCommands.isEmpty()) {
            runBlocking { syncAvailableCommands() }
        }

        val availableCommandsAtLocation = availableCommands.toMutableSet()
//        availableCommandsAtLocation.remove(SHOW_QUICK_STATS)
        availableCommandsAtLocation.addAll(
            LivePluginService.getInstance(inlayMark.project).getRegisteredLiveCommands()
        )

//        val parentMark = inlayMark.getParentSourceMark()
//        if (parentMark is MethodSourceMark) {
//            val loggerDetector = parentMark.getUserData(SourceMarkKeys.LOGGER_DETECTOR)
//            if (loggerDetector != null) {
//                runBlocking {
//                    val detectedLogs = loggerDetector.getOrFindLoggerStatements(parentMark)
//                    val logOnCurrentLine = detectedLogs.find { it.lineLocation == inlayMark.lineNumber }
//                    if (logOnCurrentLine != null) {
//                        availableCommandsAtLocation.add(WATCH_LOG)
//                    }
//                }
//            }
//
//            if (parentMark.getUserData(EndpointDetector.ENDPOINT_ID) != null) {
//                val existingQuickStats = parentMark.sourceFileMarker.getSourceMarks().find {
//                    it.artifactQualifiedName == parentMark.artifactQualifiedName
//                            && it.getUserData(ActivityQuickStatsIndicator.SHOWING_QUICK_STATS) == true
//                }
//                if (existingQuickStats == null) {
//                    availableCommandsAtLocation.add(SHOW_QUICK_STATS)
//                } else {
//                    availableCommandsAtLocation.add(HIDE_QUICK_STATS)
//                }
//            }
//        }
        return availableCommandsAtLocation.toList()
    }

    fun handleCommandInput(input: String, editor: Editor) {
        handleCommandInput(input, input, editor)
    }

    fun handleCommandInput(input: String, fullText: String, editor: Editor) {
        log.info("Processing command input: {}", input)
        (availableCommands + LivePluginService.getInstance(editor.project!!)
            .getRegisteredLiveCommands()).find { it.name == input }
            ?.let {
                val prevCommandBar = previousControlBar!!
                previousControlBar!!.dispose()
                previousControlBar = null

                val argsString = substringAfterIgnoreCase(fullText, input).trim()
                val args = if (argsString.isEmpty()) emptyList() else argsString.split(" ")

                val variableName = if (prevCommandBar.getPsiElement() is PsiDeclarationStatement) {
                    val localVar = prevCommandBar.getPsiElement().firstChild as? PsiLocalVariable
                    if (localVar != null) {
                        localVar.name
                    } else {
                        null
                    }
                } else {
                    null
                }

                val sourceMark = SourceMarkSearch.getClosestGuideMark(prevCommandBar.sourceFileMarker, editor)
                it.trigger(
                    LiveCommandContext(
                        args,
                        prevCommandBar.sourceFileMarker.psiFile.virtualFile.toFilePath().toFile(),
                        prevCommandBar.lineNumber,
                        prevCommandBar.artifactQualifiedName,
                        prevCommandBar.sourceFileMarker,
                        sourceMark,
                        prevCommandBar.getPsiElement(),
                        variableName
                    )
                )
            }
    }

    /**
     * Attempts to display live control bar below [lineNumber].
     */
    fun showControlBar(editor: Editor, lineNumber: Int, tryingAboveLine: Boolean = false) {
        //close previous control bar (if open)
        previousControlBar?.dispose(true, false)
        previousControlBar = null

        //determine control bar location
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val findInlayMark = ArtifactCreationService.getOrCreateExpressionInlayMark(fileMarker, lineNumber)
        if (findInlayMark.isPresent && canShowControlBar(findInlayMark.get().getPsiElement())) {
            val inlayMark = findInlayMark.get()
            if (fileMarker.containsSourceMark(inlayMark)) {
                if (!tryingAboveLine) {
                    //already showing inlay here, try line above
                    showControlBar(editor, lineNumber - 1, true)
                }
            } else {
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
        } else if (tryingAboveLine) {
            log.warn("No detected expression at line {}. Inlay mark ignored", lineNumber)
        } else {
            showControlBar(editor, lineNumber - 1, true)
        }
    }

    fun canShowControlBar(fileMarker: SourceFileMarker, lineNumber: Int): Boolean {
        val expressionInlayMark = ArtifactCreationService.getOrCreateExpressionInlayMark(fileMarker, lineNumber)
        return expressionInlayMark.isPresent && canShowControlBar(expressionInlayMark.get().getPsiElement())
    }

    private fun canShowControlBar(psiElement: PsiElement): Boolean {
        return when (psiElement::class.java.name) {
            "org.jetbrains.kotlin.psi.KtObjectDeclaration" -> false
            "org.jetbrains.kotlin.psi.KtProperty" -> {
                Reflect.on(psiElement).call("isLocal").get<Boolean>() == true
            }
            else -> true
        }
    }

    private fun substringAfterIgnoreCase(str: String, search: String): String {
        val index = str.indexOf(search, ignoreCase = true)
        if (index == -1) {
            return str
        }
        return str.substring(index + search.length)
    }
}
