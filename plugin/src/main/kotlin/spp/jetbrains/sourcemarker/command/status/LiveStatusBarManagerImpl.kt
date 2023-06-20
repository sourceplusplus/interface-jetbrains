/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.command.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import io.vertx.core.Vertx
import spp.jetbrains.UserData
import spp.jetbrains.artifact.service.ArtifactScopeService
import spp.jetbrains.command.util.CircularList
import spp.jetbrains.icons.PluginIcons
import spp.jetbrains.invokeLater
import spp.jetbrains.marker.SourceMarkerKeys
import spp.jetbrains.marker.SourceMarkerKeys.INSTRUMENT_EVENT_LISTENERS
import spp.jetbrains.marker.SourceMarkerKeys.INSTRUMENT_ID
import spp.jetbrains.marker.SourceMarkerKeys.INSTRUMENT_TYPE
import spp.jetbrains.marker.SourceMarkerKeys.VIEW_EVENT_LISTENERS
import spp.jetbrains.marker.SourceMarkerKeys.VIEW_SUBSCRIPTION_ID
import spp.jetbrains.marker.plugin.LiveStatusBarManager
import spp.jetbrains.marker.service.ArtifactCreationService
import spp.jetbrains.marker.service.ArtifactNamingService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import spp.jetbrains.marker.source.mark.api.component.swing.SwingSourceMarkComponentProvider
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.command.status.ui.*
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import spp.protocol.instrument.*
import spp.protocol.instrument.location.LiveSourceLocation
import spp.protocol.instrument.meter.MeterType
import spp.protocol.service.listen.LiveInstrumentListener
import spp.protocol.service.listen.LiveViewEventListener
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
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
class LiveStatusBarManagerImpl(val project: Project, val vertx: Vertx) : LiveStatusBarManager {

    private val log = logger<LiveStatusBarManagerImpl>()
    private val activeStatusBars = CopyOnWriteArrayList<LiveInstrument>()
    private val logData = ConcurrentHashMap<String, List<*>>()

    /**
     * Invoked via control bar. Force visible.
     */
    @Suppress("unused")
    override fun showBreakpointStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = SourceFileMarker.getOrCreate(editor) ?: return
        val inlayMark = ArtifactCreationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val config = SourceMarkerPlugin.getInstance(editor.project!!).getConfig()
            val location = ArtifactNamingService.getLiveSourceLocation(
                inlayMark,
                lineNumber,
                config.serviceName
            ) ?: return

            val statusBar = BreakpointStatusBar(location, inlayMark)
            inlayMark.putUserData(SourceMarkerKeys.STATE_BAR, statusBar)
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
    override fun showLogStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = SourceFileMarker.getOrCreate(editor) ?: return
        val inlayMark = ArtifactCreationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val config = SourceMarkerPlugin.getInstance(editor.project!!).getConfig()
            val location = ArtifactNamingService.getLiveSourceLocation(
                inlayMark,
                lineNumber,
                config.serviceName
            ) ?: return

            val statusBar = LogStatusBar(
                location,
                ArtifactScopeService.getScopeVariables(fileMarker.psiFile, lineNumber),
                inlayMark
            )

            inlayMark.putUserData(SourceMarkerKeys.STATE_BAR, statusBar)
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
    override fun showMeterStatusBar(editor: Editor, lineNumber: Int) {
        val fileMarker = SourceFileMarker.getOrCreate(editor) ?: return
        val inlayMark = ArtifactCreationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val config = SourceMarkerPlugin.getInstance(editor.project!!).getConfig()
            val location = ArtifactNamingService.getLiveSourceLocation(
                inlayMark,
                lineNumber,
                config.serviceName
            ) ?: return

            val statusBar = MeterStatusBar(
                location,
                ArtifactScopeService.getScopeVariables(fileMarker.psiFile, lineNumber),
                inlayMark
            )
            inlayMark.putUserData(SourceMarkerKeys.STATE_BAR, statusBar)
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
        val fileMarker = SourceFileMarker.getOrCreate(editor) ?: return
        val inlayMark = ArtifactCreationService.createExpressionInlayMark(fileMarker, lineNumber)
        if (!fileMarker.containsSourceMark(inlayMark)) {
            val wrapperPanel = JPanel()
            wrapperPanel.layout = BorderLayout()

            val config = SourceMarkerPlugin.getInstance(editor.project!!).getConfig()
            val functionIdentifier = inlayMark.artifactQualifiedName.toFunction()?.identifier
            if (functionIdentifier == null) {
                log.warn("Unable to determine function identifier for: ${inlayMark.artifactQualifiedName}")
                return
            }

            val location = LiveSourceLocation(functionIdentifier, service = config.serviceName)
            val statusBar = SpanStatusBar(location, inlayMark)
            inlayMark.putUserData(SourceMarkerKeys.STATE_BAR, statusBar)
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
        project.invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark =
                ArtifactCreationService.getOrCreateExpressionInlayMark(fileMarker, liveBreakpoint.location.line)
            if (findInlayMark.isPresent) {
                val inlayMark = findInlayMark.get()
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    inlayMark.putUserData(INSTRUMENT_ID, liveBreakpoint.id)

                    val wrapperPanel = JPanel()
                    wrapperPanel.layout = BorderLayout()

                    val statusBar = BreakpointStatusBar(liveBreakpoint.location, inlayMark)
                    inlayMark.putUserData(SourceMarkerKeys.STATE_BAR, statusBar)
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
        project.invokeLater {
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
                        ArtifactScopeService.getScopeVariables(fileMarker.psiFile, liveLog.location.line),
                        inlayMark
                    )
                    inlayMark.putUserData(SourceMarkerKeys.STATE_BAR, statusBar)
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

                    val detector = inlayMark.getUserData(SourceMarkerKeys.LOGGER_DETECTOR)!!
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
                gutterMark.get().putUserData(INSTRUMENT_TYPE, LiveInstrumentType.METER)
                when (liveMeter.meterType) {
                    MeterType.COUNT -> gutterMark.get().configuration.icon = PluginIcons.count
                    MeterType.GAUGE -> gutterMark.get().configuration.icon = PluginIcons.gauge
                    MeterType.HISTOGRAM -> gutterMark.get().configuration.icon = PluginIcons.histogram
                    else -> gutterMark.get().configuration.icon = PluginIcons.count
                }
                gutterMark.get().configuration.activateOnMouseHover = true
                gutterMark.get().configuration.activateOnMouseClick = true
                gutterMark.get().setVisible(false) //hide by default

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

                UserData.liveViewService(project)!!.addLiveView(
                    LiveView(
                        null,
                        mutableSetOf(liveMeter.id!!),
                        ArtifactQualifiedName(liveMeter.location.source, type = ArtifactType.EXPRESSION),
                        liveMeter.location,
                        LiveViewConfig(
                            "LIVE_METER",
                            listOf(liveMeter.id!!)
                        )
                    )
                ).onComplete {
                    if (it.succeeded()) {
                        gutterMark.get().putUserData(VIEW_SUBSCRIPTION_ID, it.result().subscriptionId)
                    } else {
                        log.error("Failed to add live view", it.cause())
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

    override fun removeActiveLiveInstrument(instrument: LiveInstrument) {
        activeStatusBars.remove(instrument)
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
