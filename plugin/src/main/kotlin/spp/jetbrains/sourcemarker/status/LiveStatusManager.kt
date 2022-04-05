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
package spp.jetbrains.sourcemarker.status

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker.creationService
import spp.jetbrains.marker.SourceMarker.namingService
import spp.jetbrains.marker.SourceMarker.scopeService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import spp.jetbrains.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.sourcemarker.SourceMarkerPlugin.vertx
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons.LIVE_METER_COUNT_ICON
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons.LIVE_METER_GAUGE_ICON
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons.LIVE_METER_HISTOGRAM_ICON
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.INSTRUMENT_ID
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.VIEW_EVENT_LISTENERS
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.VIEW_SUBSCRIPTION_ID
import spp.jetbrains.sourcemarker.service.InstrumentEventListener
import spp.jetbrains.sourcemarker.service.ViewEventListener
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.jetbrains.sourcemarker.status.util.CircularList
import spp.protocol.SourceServices
import spp.protocol.SourceServices.Provide.toLiveViewSubscriberAddress
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import spp.protocol.instrument.*
import spp.protocol.instrument.meter.MeterType
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import spp.protocol.view.LiveViewSubscription
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Displays and manages the live status bars inside the editor.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object LiveStatusManager : SourceMarkEventListener {

    private val log = LoggerFactory.getLogger(LiveStatusManager::class.java)
    private val activeStatusBars = CopyOnWriteArrayList<LiveInstrument>()
    private val logData = ConcurrentHashMap<String, List<*>>()

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_ADDED -> {
                if (event.sourceMark !is MethodSourceMark) return
                //todo: shouldn't need to wait for method mark added to add inlay/gutter marks
                //  should have events that just mean a method is visible

                ApplicationManager.getApplication().runReadAction {
                    val methodSourceMark = event.sourceMark as MethodSourceMark
                    val qualifiedClassName = namingService.getClassQualifiedNames(
                        methodSourceMark.sourceFileMarker.psiFile
                    )[0].identifier

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
                                is LiveMeter -> showMeterStatusIcon(it, event.sourceMark.sourceFileMarker)
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

        val inlayMark = creationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val config = Json.decodeValue(
                PropertiesComponent.getInstance(editor.project!!).getValue("sourcemarker_plugin_config"),
                SourceMarkerConfig::class.java
            )
            val statusBar = BreakpointStatusBar(
                LiveSourceLocation(
                    namingService.getClassQualifiedNames(fileMarker.psiFile)[0].identifier, lineNumber,
                    service = config.serviceName
                ),
                scopeService.getScopeVariables(fileMarker, lineNumber),
                inlayMark
            )
            inlayMark.putUserData(SourceMarkKeys.STATUS_BAR, statusBar)
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
            addStatusBar(inlayMark, statusBar)

            statusBar.focus()
        }
    }

    /**
     * Invoked via control bar. Force visible.
     */
    fun showLogStatusBar(editor: Editor, lineNumber: Int, watchExpression: Boolean) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val inlayMark = creationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val config = Json.decodeValue(
                PropertiesComponent.getInstance(editor.project!!).getValue("sourcemarker_plugin_config"),
                SourceMarkerConfig::class.java
            )
            val statusBar = LogStatusBar(
                LiveSourceLocation(
                    namingService.getClassQualifiedNames(fileMarker.psiFile)[0].identifier,
                    lineNumber,
                    service = config.serviceName
                ),
                if (watchExpression) emptyList() else scopeService.getScopeVariables(fileMarker, lineNumber),
                inlayMark,
                watchExpression
            )

            if (watchExpression) {
                val logPatterns = mutableListOf<String>()
                val parentMark = inlayMark.getParentSourceMark()
                if (parentMark is MethodSourceMark) {
                    val loggerDetector = parentMark.getUserData(SourceMarkKeys.LOGGER_DETECTOR)
                    if (loggerDetector != null) {
                        runBlocking {
                            val detectedLogs = loggerDetector.getOrFindLoggerStatements(parentMark)
                            val logOnCurrentLine = detectedLogs.find { it.lineLocation == inlayMark.lineNumber }
                            if (logOnCurrentLine != null) {
                                logPatterns.add(logOnCurrentLine.logPattern)
                            }
                        }
                    }
                }

                SourceServices.Instance.liveView!!.addLiveViewSubscription(
                    LiveViewSubscription(
                        null,
                        logPatterns,
                        ArtifactQualifiedName(
                            inlayMark.artifactQualifiedName.identifier,
                            lineNumber = inlayMark.artifactQualifiedName.lineNumber,
                            type = ArtifactType.EXPRESSION
                        ),
                        LiveSourceLocation(
                            inlayMark.artifactQualifiedName.identifier,
                            line = inlayMark.artifactQualifiedName.lineNumber!!
                        ),
                        LiveViewConfig("LOGS", listOf("endpoint_logs"))
                    )
                ).onComplete {
                    if (it.succeeded()) {
                        val subscriptionId = it.result().subscriptionId!!
                        inlayMark.putUserData(VIEW_SUBSCRIPTION_ID, subscriptionId)
                        vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(subscriptionId)) {
                            statusBar.accept(Json.decodeValue(it.body().toString(), LiveViewEvent::class.java))
                        }
                        inlayMark.addEventListener { event ->
                            if (event.eventCode == SourceMarkEventCode.MARK_REMOVED) {
                                SourceServices.Instance.liveView!!.removeLiveViewSubscription(subscriptionId)
                            }
                        }
                    } else {
                        log.error("Failed to add live view subscription", it.cause())
                    }
                }
            }

            inlayMark.putUserData(SourceMarkKeys.STATUS_BAR, statusBar)
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

            if (!watchExpression) {
                statusBar.focus()
            }
        }
    }

    fun showMeterStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val inlayMark = creationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val config = Json.decodeValue(
                PropertiesComponent.getInstance(editor.project!!).getValue("sourcemarker_plugin_config"),
                SourceMarkerConfig::class.java
            )
            val statusBar = MeterStatusBar(
                LiveSourceLocation(
                    namingService.getClassQualifiedNames(fileMarker.psiFile)[0].identifier, lineNumber,
                    service = config.serviceName
                ),
                inlayMark
            )
            inlayMark.putUserData(SourceMarkKeys.STATUS_BAR, statusBar)
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

            statusBar.focus()
        }
    }

    fun showSpanStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: {}", editor.document)
            return
        }

        val inlayMark = creationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val config = Json.decodeValue(
                PropertiesComponent.getInstance(editor.project!!).getValue("sourcemarker_plugin_config"),
                SourceMarkerConfig::class.java
            )
            val statusBar = SpanStatusBar(
                LiveSourceLocation(
                    inlayMark.artifactQualifiedName.identifier.substringBefore("#"), lineNumber,
                    service = config.serviceName
                ),
                inlayMark
            )
            inlayMark.putUserData(SourceMarkKeys.STATUS_BAR, statusBar)
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

            statusBar.focus()
        }
    }

    fun showBreakpointStatusBar(liveBreakpoint: LiveBreakpoint, fileMarker: SourceFileMarker) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark = creationService.getOrCreateExpressionInlayMark(fileMarker, liveBreakpoint.location.line)
            if (findInlayMark.isPresent) {
                val inlayMark = findInlayMark.get()
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    inlayMark.putUserData(INSTRUMENT_ID, liveBreakpoint.id)

                    val wrapperPanel = JPanel()
                    wrapperPanel.layout = BorderLayout()

                    val statusBar = BreakpointStatusBar(
                        liveBreakpoint.location,
                        emptyList(),
                        inlayMark
                    )
                    inlayMark.putUserData(SourceMarkKeys.STATUS_BAR, statusBar)
                    statusBar.setWrapperPanel(wrapperPanel)
                    wrapperPanel.add(statusBar)
                    statusBar.setEditor(editor)
                    statusBar.setLiveInstrument(liveBreakpoint)
                    editor.scrollingModel.addVisibleAreaListener(statusBar)

                    inlayMark.configuration.showComponentInlay = true
                    inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                        override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                    }
                    inlayMark.apply()
                    addStatusBar(inlayMark, statusBar)
                }
            } else {
                log.warn("No detected expression at line {}. Inlay mark ignored", liveBreakpoint.location.line)
            }
        }
    }

    fun showLogStatusBar(liveLog: LiveLog, fileMarker: SourceFileMarker) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark = creationService.getOrCreateExpressionInlayMark(fileMarker, liveLog.location.line)
            if (findInlayMark.isPresent) {
                val inlayMark = findInlayMark.get()
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    inlayMark.putUserData(INSTRUMENT_ID, liveLog.id)

                    val wrapperPanel = JPanel()
                    wrapperPanel.layout = BorderLayout()

                    val statusBar = LogStatusBar(
                        liveLog.location,
                        emptyList(),
                        inlayMark,
                        false
                    )
                    inlayMark.putUserData(SourceMarkKeys.STATUS_BAR, statusBar)
                    statusBar.setWrapperPanel(wrapperPanel)
                    wrapperPanel.add(statusBar)
                    statusBar.setEditor(editor)
                    statusBar.setLiveInstrument(liveLog)
                    editor.scrollingModel.addVisibleAreaListener(statusBar)

                    inlayMark.configuration.showComponentInlay = true
                    inlayMark.configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                        override fun makeSwingComponent(sourceMark: SourceMark): JComponent = wrapperPanel
                    }
                    inlayMark.apply()

                    val detector = inlayMark.getUserData(SourceMarkKeys.LOGGER_DETECTOR)!!
                    detector.addLiveLog(editor, inlayMark, liveLog.logFormat, liveLog.location.line)
                }
            } else {
                log.warn("No detected expression at line {}. Inlay mark ignored", liveLog.location.line)
            }
        }
    }

    @JvmStatic
    fun showMeterStatusIcon(liveMeter: LiveMeter, sourceFileMarker: SourceFileMarker) {
        //create gutter popup
        ApplicationManager.getApplication().runReadAction {
            val gutterMark = creationService.getOrCreateExpressionGutterMark(
                sourceFileMarker, liveMeter.location.line, false
            )
            if (gutterMark.isPresent) {
                gutterMark.get().putUserData(INSTRUMENT_ID, liveMeter.id!!)
                when (liveMeter.meterType) {
                    MeterType.COUNT -> gutterMark.get().configuration.icon = LIVE_METER_COUNT_ICON
                    MeterType.GAUGE -> gutterMark.get().configuration.icon = LIVE_METER_GAUGE_ICON
                    MeterType.HISTOGRAM -> gutterMark.get().configuration.icon = LIVE_METER_HISTOGRAM_ICON
                }
                gutterMark.get().configuration.activateOnMouseHover = true
                gutterMark.get().configuration.activateOnMouseClick = true

                val statusBar = LiveMeterStatusPanel(liveMeter, gutterMark.get())
                val panel = JPanel(GridBagLayout())
                panel.add(statusBar, GridBagConstraints())
                panel.preferredSize = Dimension(385, 70)
                gutterMark.get().configuration.componentProvider = object : SwingSourceMarkComponentProvider() {
                    override val defaultConfiguration: SourceMarkComponentConfiguration
                        get() = super.defaultConfiguration.apply { showAboveExpression = true }

                    override fun makeSwingComponent(sourceMark: SourceMark): JComponent = panel
                }
                gutterMark.get().apply(true)
                addStatusBar(gutterMark.get(), statusBar)

                SourceServices.Instance.liveView!!.addLiveViewSubscription(
                    LiveViewSubscription(
                        null,
                        listOf(liveMeter.toMetricId()),
                        ArtifactQualifiedName(liveMeter.location.source, type = ArtifactType.EXPRESSION),
                        liveMeter.location,
                        LiveViewConfig("LIVE_METER", listOf("last_minute", "last_hour", "last_day"))
                    )
                ).onComplete {
                    if (it.succeeded()) {
                        gutterMark.get().putUserData(VIEW_SUBSCRIPTION_ID, it.result().subscriptionId)
                    } else {
                        log.error("Failed to add live view subscription", it.cause())
                    }
                }
            } else {
                log.error("Could not create gutter mark for live meter")
            }
        }
    }

    @JvmStatic
    fun addStatusBar(sourceMark: SourceMark, listener: InstrumentEventListener) {
        if (sourceMark.getUserData(INSTRUMENT_EVENT_LISTENERS) == null) {
            sourceMark.putUserData(INSTRUMENT_EVENT_LISTENERS, mutableSetOf())
        }
        sourceMark.getUserData(INSTRUMENT_EVENT_LISTENERS)!!.add(listener)
    }

    @JvmStatic
    fun addViewEventListener(sourceMark: SourceMark, listener: ViewEventListener) {
        if (sourceMark.getUserData(VIEW_EVENT_LISTENERS) == null) {
            sourceMark.putUserData(VIEW_EVENT_LISTENERS, mutableSetOf())
        }
        sourceMark.getUserData(VIEW_EVENT_LISTENERS)!!.add(listener)
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

    fun getLogData(inlayMark: InlayMark): List<*> {
        val logId = inlayMark.getUserData(INSTRUMENT_ID)
        return logData.getOrPut(logId) { CircularList<Any>(1000) }
    }

    fun removeLogData(inlayMark: InlayMark) {
        val logId = inlayMark.getUserData(INSTRUMENT_ID)
        logData.remove(logId)
    }
}
