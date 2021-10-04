package com.sourceplusplus.sourcemarker.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.sourceplusplus.marker.SourceMarker.scopeService
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventCode
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayLogs
import com.sourceplusplus.protocol.artifact.log.LogResult
import com.sourceplusplus.protocol.instrument.LiveInstrument
import com.sourceplusplus.protocol.instrument.LiveInstrumentEvent
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import com.sourceplusplus.protocol.instrument.breakpoint.LiveBreakpoint
import com.sourceplusplus.protocol.instrument.log.LiveLog
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.BREAKPOINT_ID
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.LOG_ID
import com.sourceplusplus.sourcemarker.search.SourceMarkSearch
import com.sourceplusplus.sourcemarker.service.InstrumentEventListener
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object LiveStatusManager : SourceMarkEventListener {

    private val log = LoggerFactory.getLogger(LiveStatusManager::class.java)
    private val activeStatusBars = CopyOnWriteArrayList<LiveInstrument>()
    private val pendingEvents = CopyOnWriteArrayList<PendingInstrumentEvent>()

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
                            when (it) {
                                is LiveLog -> showLogStatusBar(it, event.sourceMark.sourceFileMarker)
                                is LiveBreakpoint -> showBreakpointStatusBar(it, event.sourceMark.sourceFileMarker)
                                else -> throw UnsupportedOperationException(it.javaClass.simpleName)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Invoked via control bar. Force visible.
     */
    fun showBreakpointStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val inlayMark = SourceMarkerUtils.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val locationSource = if ("Python" != inlayMark.language.id) {
                fileMarker.getClassQualifiedNames()[0]
            } else {
                fileMarker.psiFile.virtualFile.path //todo: ability to relative/translate
            }
            val statusBar = BreakpointStatusBar(
                LiveSourceLocation(locationSource, lineNumber),
                scopeService.getScopeVariables(fileMarker, lineNumber),
                inlayMark
            )
            statusBar.setWrapperPanel(wrapperPanel)
            wrapperPanel.add(statusBar)
            statusBar.setEditor(editor)
            editor.scrollingModel.addVisibleAreaListener(statusBar)

            inlayMark.configuration.showComponentInlay = true
            inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
            }
            inlayMark.visible.set(true)
            inlayMark.apply()

            val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
//            sourcePortal.configuration.currentPage = PageType.BREAKPOINTS
            sourcePortal.configuration.statusBar = true

            statusBar.focus()
        }
    }

    /**
     * Invoked via control bar. Force visible.
     */
    fun showLogStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val inlayMark = SourceMarkerUtils.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val locationSource = if ("Python" != inlayMark.language.id) {
                fileMarker.getClassQualifiedNames()[0]
            } else {
                fileMarker.psiFile.virtualFile.path //todo: ability to relative/translate
            }
            val statusBar = LogStatusBar(
                LiveSourceLocation(locationSource, lineNumber),
                scopeService.getScopeVariables(fileMarker, lineNumber),
                inlayMark
            )
            statusBar.setWrapperPanel(wrapperPanel)
            wrapperPanel.add(statusBar)
            statusBar.setEditor(editor)
            editor.scrollingModel.addVisibleAreaListener(statusBar)

            inlayMark.configuration.showComponentInlay = true
            inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
            }
            inlayMark.visible.set(true)
            inlayMark.apply()

            val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
            sourcePortal.configuration.currentPage = PageType.LOGS
            sourcePortal.configuration.statusBar = true

            SourceMarkerPlugin.vertx.eventBus().consumer<LogResult>(DisplayLogs(sourcePortal.portalUuid)) {
                val latestLog = it.body().logs.first()
                statusBar.setLatestLog(
                    Instant.ofEpochMilli(latestLog.timestamp.toEpochMilliseconds()), latestLog
                )
            }

            statusBar.focus()
        }
    }

    fun showBreakpointStatusBar(liveBreakpoint: LiveBreakpoint, fileMarker: SourceFileMarker) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark = SourceMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, liveBreakpoint.location.line)
            if (findInlayMark.isPresent) {
                val inlayMark = findInlayMark.get()
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    inlayMark.putUserData(BREAKPOINT_ID, liveBreakpoint.id)

                    val wrapperPanel = JPanel()
                    wrapperPanel.layout = BorderLayout()

                    val statusBar = BreakpointStatusBar(
                        liveBreakpoint.location,
                        scopeService.getScopeVariables(fileMarker, liveBreakpoint.location.line),
                        inlayMark, liveBreakpoint, editor
                    )
                    statusBar.setWrapperPanel(wrapperPanel)
                    wrapperPanel.add(statusBar)
                    editor.scrollingModel.addVisibleAreaListener(statusBar)

                    inlayMark.configuration.showComponentInlay = true
                    inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                        override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                    }
                    inlayMark.apply()

                    val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                    //sourcePortal.configuration.currentPage = PageType.BREAKPOINTS
                    sourcePortal.configuration.statusBar = true
                }
            } else {
                log.warn("No detected expression at line {}. Inlay mark ignored", liveBreakpoint.location.line)
            }
        }
    }

    fun showLogStatusBar(liveLog: LiveLog, fileMarker: SourceFileMarker) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark = SourceMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, liveLog.location.line)
            if (findInlayMark.isPresent) {
                val inlayMark = findInlayMark.get()
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    inlayMark.putUserData(LOG_ID, liveLog.id)

                    val wrapperPanel = JPanel()
                    wrapperPanel.layout = BorderLayout()

                    val statusBar = LogStatusBar(
                        liveLog.location,
                        scopeService.getScopeVariables(fileMarker, liveLog.location.line),
                        inlayMark, liveLog, editor
                    )
                    statusBar.setWrapperPanel(wrapperPanel)
                    wrapperPanel.add(statusBar)
                    editor.scrollingModel.addVisibleAreaListener(statusBar)

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
                            Instant.ofEpochMilli(latestLog.timestamp.toEpochMilliseconds()), latestLog
                        )
                    }
                }
            } else {
                log.warn("No detected expression at line {}. Inlay mark ignored", liveLog.location.line)
            }
        }
    }

    fun addStatusBar(inlayMark: InlayMark, listener: InstrumentEventListener) {
        if (inlayMark.getUserData(INSTRUMENT_EVENT_LISTENERS) == null) {
            inlayMark.putUserData(INSTRUMENT_EVENT_LISTENERS, ArrayList())
        }
        inlayMark.getUserData(INSTRUMENT_EVENT_LISTENERS)!!.add(listener)

        val instrumentId = inlayMark.getUserData(BREAKPOINT_ID) ?: inlayMark.getUserData(LOG_ID)
        pendingEvents.removeIf {
            if (instrumentId == it.instrumentId) {
                listener.accept(it.event)
                true
            } else {
                false
            }
        }
    }

    fun addPendingEvent(event: LiveInstrumentEvent, instrumentId: String) {
        pendingEvents.add(PendingInstrumentEvent(instrumentId, event))
        pendingEvents.removeIf { event ->
            val sourceMark = SourceMarkSearch.findByInstrumentId(event.instrumentId)
            if (sourceMark != null) {
                val eventListeners = sourceMark.getUserData(INSTRUMENT_EVENT_LISTENERS)
                if (eventListeners?.isNotEmpty() == true) {
                    eventListeners.forEach { it.accept(event.event) }
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    fun addActiveLiveInstrument(instrument: LiveInstrument) {
        activeStatusBars.add(instrument)
    }

    fun addActiveLiveInstruments(instruments: List<LiveInstrument>) {
        activeStatusBars.addAll(instruments)
    }

    fun removeActiveLiveInstrument(instrument: LiveInstrument) {
        activeStatusBars.remove(instrument)
    }

    fun removeActiveLiveInstrument(instrumentId: String) {
        activeStatusBars.removeIf { it.id == instrumentId }
    }

    data class PendingInstrumentEvent(
        val instrumentId: String,
        val event: LiveInstrumentEvent
    )
}
