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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker.creationService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ClassSourceMark
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.sourcemarker.ControlBar
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.command.LiveControlCommand.*
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.status.LiveStatusManager
import spp.protocol.ProtocolAddress.Global.SetCurrentPage
import spp.protocol.SourceMarkerServices
import spp.protocol.developer.SelfInfo
import spp.protocol.portal.PageType
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
    private val availableCommands by lazy {
        runBlocking {
            val future = Promise.promise<SelfInfo>()
            SourceMarkerServices.Instance.liveService!!.getSelf(future)
            val selfInfo = future.future().await()
            LiveControlCommand.values().toList().filter {
                @Suppress("UselessCallOnCollection") //unknown enums are null
                selfInfo.permissions.filterNotNull().map { it.name }.contains(it.name)
            }
        }
    }

    fun handleCommandInput(input: String, editor: Editor) {
        log.info("Processing command input: {}", input)
        when (input) {
            VIEW_ACTIVITY.command -> handleViewPortalCommand(editor, VIEW_ACTIVITY)
            VIEW_TRACES.command -> handleViewPortalCommand(editor, VIEW_TRACES)
            VIEW_LOGS.command -> handleViewPortalCommand(editor, VIEW_LOGS)
            ADD_LIVE_BREAKPOINT.command -> {
                //replace command inlay with breakpoint status inlay
                val prevCommandBar = previousControlBar!!
                previousControlBar!!.dispose()
                previousControlBar = null

                ApplicationManager.getApplication().runWriteAction {
                    LiveStatusManager.showBreakpointStatusBar(editor, prevCommandBar.lineNumber)
                }
            }
            ADD_LIVE_LOG.command -> {
                //replace command inlay with log status inlay
                val prevCommandBar = previousControlBar!!
                previousControlBar!!.dispose()
                previousControlBar = null

                ApplicationManager.getApplication().runWriteAction {
                    LiveStatusManager.showLogStatusBar(editor, prevCommandBar.lineNumber)
                }
            }
            ADD_LIVE_METER.command -> {
                //replace command inlay with meter status inlay
                val prevCommandBar = previousControlBar!!
                previousControlBar!!.dispose()
                previousControlBar = null

                ApplicationManager.getApplication().runWriteAction {
                    LiveStatusManager.showMeterStatusBar(editor, prevCommandBar.lineNumber)
                }
            }
            ADD_LIVE_SPAN.command -> {
                //replace command inlay with span status inlay
                val prevCommandBar = previousControlBar!!
                previousControlBar!!.dispose()
                previousControlBar = null

                ApplicationManager.getApplication().runWriteAction {
                    LiveStatusManager.showSpanStatusBar(editor, prevCommandBar.lineNumber)
                }
            }
            CLEAR_LIVE_BREAKPOINTS.command -> {
                previousControlBar!!.dispose()
                previousControlBar = null

                SourceMarkerServices.Instance.liveInstrument!!.clearLiveBreakpoints {
                    if (it.failed()) {
                        log.error("Failed to clear live breakpoints", it.cause())
                    }
                }
            }
            CLEAR_LIVE_LOGS.command -> {
                previousControlBar!!.dispose()
                previousControlBar = null

                SourceMarkerServices.Instance.liveInstrument!!.clearLiveLogs {
                    if (it.failed()) {
                        log.error("Failed to clear live logs", it.cause())
                    }
                }
            }
            CLEAR_LIVE_INSTRUMENTS.command -> {
                previousControlBar!!.dispose()
                previousControlBar = null

                SourceMarkerServices.Instance.liveInstrument!!.clearLiveInstruments {
                    if (it.failed()) {
                        log.error("Failed to clear live instruments", it.cause())
                    }
                }
            }
            else -> throw UnsupportedOperationException("Command input: $input")
        }
    }

    private fun handleViewPortalCommand(editor: Editor, command: LiveControlCommand) {
        var classSourceMark: ClassSourceMark? = null
        val sourceMark = previousControlBar!!.sourceFileMarker.getSourceMarks().find {
            if (it is ClassSourceMark) {
                classSourceMark = it //todo: probably doesn't handle inner classes well
                false
            } else if (it is MethodSourceMark) {
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

        if (sourceMark != null) {
            val portal = sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
            when (command) {
                VIEW_ACTIVITY -> portal.configuration.currentPage = PageType.ACTIVITY
                VIEW_TRACES -> portal.configuration.currentPage = PageType.TRACES
                VIEW_LOGS -> portal.configuration.currentPage = PageType.LOGS
                else -> throw UnsupportedOperationException("Command input: $command")
            }
            SourceMarkerPlugin.vertx.eventBus().request<Any>(SetCurrentPage, portal) {
                sourceMark.triggerEvent(SourceMarkEvent(sourceMark, SourceMarkEventCode.PORTAL_OPENING))
            }
        } else if (classSourceMark != null) {
            val portal = classSourceMark!!.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
            when (command) {
                VIEW_ACTIVITY -> portal.configuration.currentPage = PageType.ACTIVITY
                VIEW_TRACES -> portal.configuration.currentPage = PageType.TRACES
                VIEW_LOGS -> portal.configuration.currentPage = PageType.LOGS
                else -> throw UnsupportedOperationException("Command input: $command")
            }
            SourceMarkerPlugin.vertx.eventBus().request<Any>(SetCurrentPage, portal) {
                classSourceMark!!.triggerEvent(SourceMarkEvent(classSourceMark!!, SourceMarkEventCode.PORTAL_OPENING))
            }
        } else {
            log.warn("No source mark found for command: {}", command)
        }

        previousControlBar!!.dispose()
        previousControlBar = null
    }

    fun canShowControlBar(fileMarker: SourceFileMarker, lineNumber: Int): Boolean {
        return creationService.getOrCreateExpressionInlayMark(fileMarker, lineNumber).isPresent
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

        val findInlayMark = creationService.getOrCreateExpressionInlayMark(fileMarker, lineNumber)
        if (findInlayMark.isPresent) {
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

                val controlBar = ControlBar(editor, inlayMark, availableCommands)
                wrapperPanel.add(controlBar)
                editor.scrollingModel.addVisibleAreaListener(controlBar)

                inlayMark.configuration.showComponentInlay = true
                inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                    override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                }
                inlayMark.visible.set(true)
                inlayMark.apply()

                val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                sourcePortal.configuration.currentPage = PageType.LOGS
                sourcePortal.configuration.statusBar = true

                controlBar.focus()
            }
        } else if (tryingAboveLine) {
            log.warn("No detected expression at line {}. Inlay mark ignored", lineNumber)
        } else {
            showControlBar(editor, lineNumber - 1, true)
        }
    }
}
