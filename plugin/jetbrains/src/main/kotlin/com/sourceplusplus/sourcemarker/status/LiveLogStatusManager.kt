package com.sourceplusplus.sourcemarker.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayLogs
import com.sourceplusplus.protocol.artifact.log.LogResult
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import com.sourceplusplus.protocol.instrument.log.LiveLog
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object LiveLogStatusManager : SourceMarkEventListener {

    private val log = LoggerFactory.getLogger(LiveLogStatusManager::class.java)
    private val activeStatusBars = CopyOnWriteArrayList<LiveLog>()

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_ADDED -> {
                if (event.sourceMark !is MethodSourceMark) return
                //todo: shouldn't need to wait for method mark added to add inlay marks
                //  should have events that just mean a method is visible

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
                            showStatusBar(it, event.sourceMark.sourceFileMarker)
                        }
                    }
                }
            }
        }
    }

    fun showStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val findInlayMark = SourceMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, lineNumber)
        if (findInlayMark.isPresent) {
            val inlayMark = findInlayMark.get()
            if (!fileMarker.containsSourceMark(inlayMark)) {
                val wrapperPanel = JPanel()
                wrapperPanel.layout = BorderLayout()

                val qualifiedClassName = fileMarker.getClassQualifiedNames()[0]
                val statusBar = LogStatusBar(
                    LiveSourceLocation(qualifiedClassName, lineNumber),
                    inlayMark
                )
                wrapperPanel.add(statusBar)
                statusBar.setEditor(editor)

                inlayMark.configuration.showComponentInlay = true
                inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                    override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                }
                inlayMark.apply()

                val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                sourcePortal.configuration.currentPage = PageType.LOGS
                sourcePortal.configuration.statusBar = true

                SourceMarkerPlugin.vertx.eventBus().consumer<LogResult>(DisplayLogs(sourcePortal.portalUuid)) {
                    val latestLog = it.body().logs.first()
                    statusBar.setLatestLog(
                        Instant.ofEpochMilli(latestLog.timestamp.toEpochMilliseconds()),
                        latestLog.getFormattedMessage()
                    )
                }

                statusBar.focus()
            }
        } else {
            log.warn("No detected expression at line {}. Inlay mark ignored", lineNumber)
        }
    }

    fun showStatusBar(liveLog: LiveLog, fileMarker: SourceFileMarker) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark = SourceMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, liveLog.location.line)
            if (findInlayMark.isPresent) {
                val inlayMark = findInlayMark.get()
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    val wrapperPanel = JPanel()
                    wrapperPanel.layout = BorderLayout()

                    val statusBar = LogStatusBar(liveLog.location, inlayMark, liveLog, editor)
                    wrapperPanel.add(statusBar)

                    inlayMark.configuration.showComponentInlay = true
                    inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                        override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                    }
                    inlayMark.apply()

                    val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                    sourcePortal.configuration.currentPage = PageType.LOGS
                    sourcePortal.configuration.statusBar = true

                    val detector = inlayMark.getUserData(SourceMarkKeys.LOGGER_DETECTOR)!!
                    detector.addLiveLog(editor, inlayMark, liveLog.logFormat, liveLog.location.line)

                    SourceMarkerPlugin.vertx.eventBus().consumer<LogResult>(DisplayLogs(sourcePortal.portalUuid)) {
                        val latestLog = it.body().logs.first()
                        statusBar.setLatestLog(
                            Instant.ofEpochMilli(latestLog.timestamp.toEpochMilliseconds()),
                            latestLog.getFormattedMessage()
                        )
                    }
                }
            } else {
                log.warn("No detected expression at line {}. Inlay mark ignored", liveLog.location.line)
            }
        }
    }

    fun addActiveLiveLog(liveLog: LiveLog) {
        activeStatusBars.add(liveLog)
    }

    fun addActiveLiveLogs(liveLogs: List<LiveLog>) {
        activeStatusBars.addAll(liveLogs)
    }

    fun removeActiveLiveLog(liveLog: LiveLog) {
        activeStatusBars.remove(liveLog)
    }
}
