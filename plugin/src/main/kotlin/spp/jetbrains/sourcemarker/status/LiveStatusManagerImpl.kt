/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.UserData
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.marker.impl.ArtifactCreationService
import spp.jetbrains.marker.impl.ArtifactNamingService
import spp.jetbrains.marker.impl.ArtifactScopeService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import spp.jetbrains.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.plugin.LiveStatusManager
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.INSTRUMENT_ID
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.VIEW_EVENT_LISTENERS
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.VIEW_SUBSCRIPTION_ID
import spp.jetbrains.sourcemarker.status.util.CircularList
import spp.protocol.SourceServices.Provide.toLiveViewSubscriberAddress
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import spp.protocol.instrument.*
import spp.protocol.instrument.meter.MeterType
import spp.protocol.service.listen.LiveInstrumentListener
import spp.protocol.service.listen.LiveViewEventListener
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
class LiveStatusManagerImpl(val project: Project, val vertx: Vertx) : LiveStatusManager, SourceMarkEventListener {

    companion object {
        private val log = logger<LiveStatusManagerImpl>()
    }

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
                    val qualifiedClassNames = ArtifactNamingService.getQualifiedClassNames(
                        methodSourceMark.sourceFileMarker.psiFile
                    )
                    if (qualifiedClassNames.isEmpty()) return@runReadAction

                    val qualifiedClassName = qualifiedClassNames[0].identifier
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
    @Suppress("unused")
    override fun showBreakpointStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: ${editor.document}")
            return
        }

        val inlayMark = ArtifactCreationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val classNames = ArtifactNamingService.getQualifiedClassNames(fileMarker.psiFile)
            val qualifiedClassNameOrFilename = if (classNames.isNotEmpty()) {
                classNames[0].identifier
            } else {
                fileMarker.psiFile.virtualFile.name
            }
            val config = SourceMarkerPlugin.getInstance(editor.project!!).getConfig()
            val statusBar = BreakpointStatusBar(
                LiveSourceLocation(qualifiedClassNameOrFilename, lineNumber, service = config.serviceName),
                ArtifactScopeService.getScopeVariables(fileMarker, lineNumber),
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
    @Suppress("unused")
    override fun showLogStatusBar(editor: Editor, lineNumber: Int, watchExpression: Boolean) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: ${editor.document}")
            return
        }

        val inlayMark = ArtifactCreationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val classNames = ArtifactNamingService.getQualifiedClassNames(fileMarker.psiFile)
            val qualifiedClassNameOrFilename = if (classNames.isNotEmpty()) {
                classNames[0].identifier
            } else {
                fileMarker.psiFile.virtualFile.name
            }
            val config = SourceMarkerPlugin.getInstance(editor.project!!).getConfig()
            val statusBar = LogStatusBar(
                LiveSourceLocation(
                    qualifiedClassNameOrFilename, lineNumber,
                    service = config.serviceName
                ),
                if (watchExpression) emptyList() else ArtifactScopeService.getScopeVariables(fileMarker, lineNumber),
                inlayMark,
                watchExpression
            )

            if (watchExpression) {
                val logPatterns = mutableListOf<String>()
                val parentMark = inlayMark.getParentSourceMark()
                if (parentMark is MethodSourceMark) {
                    val loggerDetector = parentMark.getUserData(SourceMarkKeys.LOGGER_DETECTOR)
                    if (loggerDetector != null) {
                        safeRunBlocking {
                            val detectedLogs = loggerDetector.getOrFindLoggerStatements(parentMark)
                            val logOnCurrentLine = detectedLogs.find { it.lineLocation == inlayMark.lineNumber }
                            if (logOnCurrentLine != null) {
                                logPatterns.add(logOnCurrentLine.logPattern)
                            }
                        }
                    }
                }

                UserData.liveViewService(project)!!.addLiveViewSubscription(
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
                            statusBar.accept(LiveViewEvent(it.body()))
                        }
                        inlayMark.addEventListener { event ->
                            if (event.eventCode == SourceMarkEventCode.MARK_REMOVED) {
                                UserData.liveViewService(project)!!.removeLiveViewSubscription(subscriptionId)
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

    @Suppress("unused")
    override fun showMeterStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: ${editor.document}")
            return
        }

        val inlayMark = ArtifactCreationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val classNames = ArtifactNamingService.getQualifiedClassNames(fileMarker.psiFile)
            val qualifiedClassNameOrFilename = if (classNames.isNotEmpty()) {
                classNames[0].identifier
            } else {
                fileMarker.psiFile.virtualFile.name
            }
            val config = SourceMarkerPlugin.getInstance(editor.project!!).getConfig()
            val statusBar = MeterStatusBar(
                LiveSourceLocation(
                    qualifiedClassNameOrFilename, lineNumber,
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

    @Suppress("unused")
    override fun showSpanStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
            .getUserData(SourceFileMarker.KEY)
        if (fileMarker == null) {
            log.warn("Could not find file marker for file: ${editor.document}")
            return
        }

        val inlayMark = ArtifactCreationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val classNames = ArtifactNamingService.getQualifiedClassNames(fileMarker.psiFile)
            val qualifiedClassNameOrFilename = if (classNames.isNotEmpty()) {
                classNames[0].identifier
            } else {
                fileMarker.psiFile.virtualFile.name
            }
            val config = SourceMarkerPlugin.getInstance(editor.project!!).getConfig()
            val statusBar = SpanStatusBar(
                LiveSourceLocation(
                    qualifiedClassNameOrFilename, lineNumber,
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

    override fun showBreakpointStatusBar(liveBreakpoint: LiveBreakpoint, fileMarker: SourceFileMarker) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark =
                ArtifactCreationService.getOrCreateExpressionInlayMark(fileMarker, liveBreakpoint.location.line)
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
                log.warn("No detected expression at line ${liveBreakpoint.location.line}. Inlay mark ignored")
            }
        }
    }

    override fun showLogStatusBar(liveLog: LiveLog, fileMarker: SourceFileMarker) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark =
                ArtifactCreationService.getOrCreateExpressionInlayMark(fileMarker, liveLog.location.line)
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
                log.warn("No detected expression at line ${liveLog.location.line}. Inlay mark ignored")
            }
        }
    }

    override fun showMeterStatusIcon(liveMeter: LiveMeter, sourceFileMarker: SourceFileMarker) {
        //create gutter popup
        ApplicationManager.getApplication().runReadAction {
            val gutterMark = ArtifactCreationService.getOrCreateExpressionGutterMark(
                sourceFileMarker, liveMeter.location.line, false
            )
            if (gutterMark.isPresent) {
                gutterMark.get().putUserData(INSTRUMENT_ID, liveMeter.id!!)
                when (liveMeter.meterType) {
                    MeterType.COUNT -> gutterMark.get().configuration.icon = PluginIcons.count
                    MeterType.GAUGE -> gutterMark.get().configuration.icon = PluginIcons.gauge
                    MeterType.HISTOGRAM -> gutterMark.get().configuration.icon = PluginIcons.histogram
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

                UserData.liveViewService(project)!!.addLiveViewSubscription(
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

    override fun addStatusBar(sourceMark: SourceMark, listener: LiveInstrumentListener) {
        if (sourceMark.getUserData(INSTRUMENT_EVENT_LISTENERS) == null) {
            sourceMark.putUserData(INSTRUMENT_EVENT_LISTENERS, mutableSetOf())
        }
        sourceMark.getUserData(INSTRUMENT_EVENT_LISTENERS)!!.add(listener)
    }

    override fun addViewEventListener(sourceMark: SourceMark, listener: LiveViewEventListener) {
        if (sourceMark.getUserData(VIEW_EVENT_LISTENERS) == null) {
            sourceMark.putUserData(VIEW_EVENT_LISTENERS, mutableSetOf())
        }
        sourceMark.getUserData(VIEW_EVENT_LISTENERS)!!.add(listener)
    }

    override fun addActiveLiveInstrument(instrument: LiveInstrument) {
        activeStatusBars.add(instrument)
    }

    override fun addActiveLiveInstruments(instruments: List<LiveInstrument>) {
        activeStatusBars.addAll(instruments)
    }

    override fun removeActiveLiveInstrument(instrument: LiveInstrument) {
        activeStatusBars.remove(instrument)
    }

    fun removeActiveLiveInstrument(instrumentId: String) {
        activeStatusBars.removeIf { it.id == instrumentId }
    }

    override fun getLogData(inlayMark: InlayMark): List<*> {
        val logId = inlayMark.getUserData(INSTRUMENT_ID)
        return logData.getOrPut(logId) { CircularList<Any>(1000) }
    }

    override fun removeLogData(inlayMark: InlayMark) {
        val logId = inlayMark.getUserData(INSTRUMENT_ID)
        logData.remove(logId)
    }
}
