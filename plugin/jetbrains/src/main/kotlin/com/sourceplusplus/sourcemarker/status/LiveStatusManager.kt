package com.sourceplusplus.sourcemarker.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiVariable
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.processor.VariablesProcessor
import com.intellij.psi.scope.util.PsiScopesUtil
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
import com.sourceplusplus.protocol.instrument.LiveInstrument
import com.sourceplusplus.protocol.instrument.LiveSourceLocation
import com.sourceplusplus.protocol.instrument.breakpoint.LiveBreakpoint
import com.sourceplusplus.protocol.instrument.log.LiveLog
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.LOG_ID
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
object LiveStatusManager : SourceMarkEventListener {

    private val log = LoggerFactory.getLogger(LiveStatusManager::class.java)
    private val activeStatusBars = CopyOnWriteArrayList<LiveInstrument>()

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

            //determine available vars
            val scopeVars = mutableListOf<String>()
            val minScope = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber - 1)!!
            val variablesProcessor: VariablesProcessor = object : VariablesProcessor(false) {
                override fun check(`var`: PsiVariable, state: ResolveState): Boolean = true
            }
            PsiScopesUtil.treeWalkUp(variablesProcessor, minScope, null)
            for (i in 0 until variablesProcessor.size()) {
                scopeVars.add(variablesProcessor.getResult(i).name!!)
            }

            val qualifiedClassName = fileMarker.getClassQualifiedNames()[0]
            val statusBar = BreakpointStatusBar(
                LiveSourceLocation(qualifiedClassName, lineNumber),
                scopeVars,
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

            //determine available vars
            val scopeVars = mutableListOf<String>()
            val minScope = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber - 1)!!
            val variablesProcessor: VariablesProcessor = object : VariablesProcessor(false) {
                override fun check(`var`: PsiVariable, state: ResolveState): Boolean = true
            }
            PsiScopesUtil.treeWalkUp(variablesProcessor, minScope, null)
            for (i in 0 until variablesProcessor.size()) {
                scopeVars.add(variablesProcessor.getResult(i).name!!)
            }

            val qualifiedClassName = fileMarker.getClassQualifiedNames()[0]
            val statusBar = LogStatusBar(
                LiveSourceLocation(qualifiedClassName, lineNumber),
                scopeVars,
                inlayMark
            )
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

                    //determine available vars
                    val scopeVars = mutableListOf<String>()
                    val minScope = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, liveLog.location.line - 1)!!
                    val variablesProcessor: VariablesProcessor = object : VariablesProcessor(false) {
                        override fun check(`var`: PsiVariable, state: ResolveState): Boolean = true
                    }
                    PsiScopesUtil.treeWalkUp(variablesProcessor, minScope, null)
                    for (i in 0 until variablesProcessor.size()) {
                        scopeVars.add(variablesProcessor.getResult(i).name!!)
                    }

                    val statusBar = LogStatusBar(liveLog.location, scopeVars, inlayMark, liveLog, editor)
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
}
