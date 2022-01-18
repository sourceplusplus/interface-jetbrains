package spp.jetbrains.sourcemarker.status

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import io.vertx.core.json.Json
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
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons.LIVE_METER_COUNT_ICON
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons.LIVE_METER_GAUGE_ICON
import spp.jetbrains.sourcemarker.icons.SourceMarkerIcons.LIVE_METER_HISTOGRAM_ICON
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.BREAKPOINT_ID
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.LOG_ID
import spp.jetbrains.sourcemarker.service.InstrumentEventListener
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.ProtocolAddress.Portal.DisplayLogs
import spp.protocol.SourceMarkerServices
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import spp.protocol.artifact.log.LogResult
import spp.protocol.instrument.LiveInstrument
import spp.protocol.instrument.LiveSourceLocation
import spp.protocol.instrument.breakpoint.LiveBreakpoint
import spp.protocol.instrument.log.LiveLog
import spp.protocol.instrument.meter.LiveMeter
import spp.protocol.instrument.meter.MeterType
import spp.protocol.portal.PageType
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewSubscription
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.time.Instant
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
    private val allStatusBars = CopyOnWriteArrayList<StatusBar>()

    override fun handleEvent(event: SourceMarkEvent) {
        when (event.eventCode) {
            SourceMarkEventCode.MARK_ADDED -> {
                if (event.sourceMark !is MethodSourceMark) return
                //todo: shouldn't need to wait for method mark added to add inlay marks
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

            val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
//            sourcePortal.configuration.currentPage = PageType.BREAKPOINTS
            sourcePortal.configuration.statusBar = true

            statusBar.focus()
            allStatusBars.add(statusBar)
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
            allStatusBars.add(statusBar)
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

            val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
//            sourcePortal.configuration.currentPage = PageType.METERS
            sourcePortal.configuration.statusBar = true

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

            val sourcePortal = inlayMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
//            sourcePortal.configuration.currentPage = PageType.METERS
            sourcePortal.configuration.statusBar = true

            statusBar.focus()
            allStatusBars.add(statusBar)
        }
    }

    fun showBreakpointStatusBar(liveBreakpoint: LiveBreakpoint, fileMarker: SourceFileMarker) {
        ApplicationManager.getApplication().invokeLater {
            val editor = FileEditorManager.getInstance(fileMarker.project).selectedTextEditor!!
            val findInlayMark = creationService.getOrCreateExpressionInlayMark(fileMarker, liveBreakpoint.location.line)
            if (findInlayMark.isPresent) {
                val inlayMark = findInlayMark.get()
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    inlayMark.putUserData(BREAKPOINT_ID, liveBreakpoint.id)

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
            val findInlayMark = creationService.getOrCreateExpressionInlayMark(fileMarker, liveLog.location.line)
            if (findInlayMark.isPresent) {
                val inlayMark = findInlayMark.get()
                if (!fileMarker.containsSourceMark(inlayMark)) {
                    inlayMark.putUserData(LOG_ID, liveLog.id)

                    val wrapperPanel = JPanel()
                    wrapperPanel.layout = BorderLayout()

                    val statusBar = LogStatusBar(
                        liveLog.location,
                        emptyList(),
                        inlayMark
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

    @JvmStatic
    fun showMeterStatusIcon(liveMeter: LiveMeter, sourceFileMarker: SourceFileMarker) {
        SourceMarkerServices.Instance.liveView!!.addLiveViewSubscription(
            LiveViewSubscription(
                null,
                listOf(liveMeter.toMetricId()),
                ArtifactQualifiedName(liveMeter.location.source, type = ArtifactType.EXPRESSION),
                liveMeter.location,
                LiveViewConfig(
                    "LIVE_METER",
                    true,
                    listOf("last_minute", "last_hour", "last_day"),
                    0
                )
            )
        ) {
            if (it.failed()) {
                log.error("Failed to add live view subscription", it.cause())
            }
        }

        //create gutter popup
        ApplicationManager.getApplication().runReadAction {
            val gutterMark = creationService.getOrCreateExpressionGutterMark(
                sourceFileMarker, liveMeter.location.line, false
            )
            if (gutterMark.isPresent) {
                gutterMark.get().putUserData(SourceMarkKeys.METER_ID, liveMeter.id!!)
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

    fun removeInactiveStatusBars() {
        allStatusBars.removeIf {
            if (!it.isActive()) {
                it.dispose()
                true
            } else {
                false
            }
        }
    }

    fun removeStatusBar(statusBar: StatusBar) {
        allStatusBars.remove(statusBar)
    }
}
