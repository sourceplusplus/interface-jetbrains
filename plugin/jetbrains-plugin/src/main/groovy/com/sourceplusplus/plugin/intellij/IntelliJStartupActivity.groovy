package com.sourceplusplus.plugin.intellij

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.PluginSourceFile
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.coordinate.artifact.track.FileClosedTracker
import com.sourceplusplus.plugin.intellij.inspect.IntelliJInspectionProvider
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.render.SourceArtifactLineMarkerGutterIconRenderer
import com.sourceplusplus.plugin.intellij.settings.application.ApplicationSettingsDialogWrapper
import com.sourceplusplus.plugin.intellij.settings.connect.ConnectDialogWrapper
import com.sourceplusplus.plugin.intellij.source.navigate.IntelliJArtifactNavigator
import com.sourceplusplus.plugin.intellij.tool.SourcePluginConsoleService
import com.sourceplusplus.plugin.intellij.util.IntelliUtils
import com.sourceplusplus.tooltip.coordinate.track.TooltipViewTracker
import com.sourceplusplus.tooltip.display.TooltipUI
import io.vertx.core.Vertx
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.PatternLayout
import org.apache.log4j.spi.LoggingEvent
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import javax.swing.event.HyperlinkEvent
import java.util.concurrent.CountDownLatch

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJStartupActivity implements StartupActivity {

    //todo: fix https://github.com/CodeBrig/Source/issues/1 and remove static block below
    static {
        ConsoleAppender console = new ConsoleAppender()
        console.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"))
        console.setThreshold(Level.DEBUG)
        console.activateOptions()

        org.apache.log4j.Logger.rootLogger.loggerRepository.resetConfiguration()
        org.apache.log4j.Logger.getLogger("com.sourceplusplus").addAppender(console)
    }

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static EditorMouseMotionListener editorMouseMotionListener
    private static SourcePlugin sourcePlugin
    public static Project currentProject

    @Override
    void runActivity(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return //don't need to boot everything for unit tests
        }
        System.setProperty("vertx.disableFileCPResolving", "true")

        //redirect loggers to console
        def consoleView = ServiceManager.getService(project, SourcePluginConsoleService.class).getConsoleView()
        org.apache.log4j.Logger.getLogger("com.sourceplusplus").addAppender(new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent loggingEvent) {
                Object message = loggingEvent.message
                if (loggingEvent.level.isGreaterOrEqual(Level.WARN)) {
                    if (message.toString().startsWith("[TOOLTIP]")) {
                        consoleView.print(message.toString() + "\n", ConsoleViewContentType.ERROR_OUTPUT)
                    } else {
                        consoleView.print("[PLUGIN] - " + message.toString() + "\n", ConsoleViewContentType.ERROR_OUTPUT)
                    }
                } else if (loggingEvent.level.isGreaterOrEqual(Level.INFO)) {
                    if (message.toString().startsWith("[TOOLTIP]")) {
                        consoleView.print(message.toString() + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    } else {
                        consoleView.print("[PLUGIN] - " + message.toString() + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                }
            }

            @Override
            void close() {
            }

            @Override
            boolean requiresLayout() {
                return false
            }
        })

        currentProject = project
        if (sourcePlugin != null) {
            CountDownLatch latch = new CountDownLatch(1)
            sourcePlugin.vertx.close({
                latch.countDown()
            })
            latch.await()
        }

        def coreClient = new SourceCoreClient(SourcePluginConfig.current.sppUrl)
        if (SourcePluginConfig.current.apiKey != null) {
            coreClient.apiKey = SourcePluginConfig.current.apiKey
        }

        coreClient.info({
            if (it.failed()) {
                Notifications.Bus.notify(
                        new Notification("Source++", "Connection Required",
                                "Source++ must be connected to a valid host to activate. Please <a href=\"#\">connect</a> here.",
                                NotificationType.INFORMATION, new NotificationListener() {
                            @Override
                            void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                                def connectDialog = new ConnectDialogWrapper(project)
                                connectDialog.createCenterPanel()
                                connectDialog.show()

                                if (connectDialog.startPlugin) {
                                    coreClient = new SourceCoreClient(SourcePluginConfig.current.sppUrl)
                                    if (SourcePluginConfig.current.apiKey != null) {
                                        coreClient.apiKey = SourcePluginConfig.current.apiKey
                                    }
                                    coreClient.ping({
                                        if (it.succeeded()) {
                                            if (SourcePluginConfig.current.appUuid == null) {
                                                doApplicationSettingsDialog(project, coreClient)
                                            } else {
                                                coreClient.getApplication(SourcePluginConfig.current.appUuid, {
                                                    if (it.failed() || !it.result().isPresent()) {
                                                        SourcePluginConfig.current.appUuid = null
                                                        doApplicationSettingsDialog(project, coreClient)
                                                    } else {
                                                        startSourcePlugin(coreClient)
                                                    }
                                                })
                                            }
                                        }
                                    })
                                }
                            }
                        })
                )
            } else {
                if (SourcePluginConfig.current.appUuid == null) {
                    doApplicationSettingsDialog(project, coreClient)
                } else {
                    coreClient.getApplication(SourcePluginConfig.current.appUuid, {
                        if (it.failed() || !it.result().isPresent()) {
                            SourcePluginConfig.current.appUuid = null
                            doApplicationSettingsDialog(project, coreClient)
                        } else {
                            startSourcePlugin(coreClient)
                        }
                    })
                }
            }
        })

        //setup file listening
        def messageBus = project.getMessageBus()
        messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {

            @Override
            void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            }

            @Override
            void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                log.debug("File closed: {}", file)
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    void run() {
                        def fileMarker = file.getUserData(IntelliJSourceFileMarker.KEY)
                        if (fileMarker != null) {
                            file.putUserData(IntelliJSourceFileMarker.KEY, null)
                            sourcePlugin.vertx.eventBus().send(FileClosedTracker.ADDRESS, fileMarker)
                        }
                    }
                })
                if (file == IntelliJInspectionProvider.lastFileOpened?.virtualFile) {
                    IntelliJInspectionProvider.lastFileOpened = null
                }
            }
        })
        log.info("Source++ loaded for project: {} ({})", project.name, project.getPresentableUrl())
    }

    static void startSourcePlugin(SourceCoreClient coreClient) {
        //start plugin
        sourcePlugin = new SourcePlugin(Objects.requireNonNull(coreClient))
        registerCodecs()
        coreClient.registerIP()

        //register coordinators
        sourcePlugin.vertx.deployVerticle(new IntelliJArtifactNavigator())

        //todo: better
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            void run() {
                IntelliJInspectionProvider.PENDING_FILE_VISITS.removeIf({
                    log.debug("Visited by file: {} - Pending: true", it.virtualFile)
                    coordinateSourceFileOpened(sourcePlugin, it)
                    return true
                })
            }
        })

        sourcePlugin.vertx.eventBus().consumer(TooltipUI.TOOLTIP_READY, {
            //set tooltip theme
            UIManager.addPropertyChangeListener({
                if (it.newValue instanceof IntelliJLaf) {
                    TooltipUI.updateTheme(false)
                } else {
                    TooltipUI.updateTheme(true)
                }
            })
            if (UIManager.lookAndFeel instanceof IntelliJLaf) {
                TooltipUI.updateTheme(false)
            } else {
                TooltipUI.updateTheme(true)
            }
        })
    }

    private static void doApplicationSettingsDialog(Project project, SourceCoreClient coreClient) {
        Notifications.Bus.notify(
                new Notification("Source++", "Application Required",
                        "Last step, Source++ also requires this project to be linked with a new or existing application. Please <a href=\"#\">choose</a> here.",
                        NotificationType.INFORMATION, new NotificationListener() {
                    @Override
                    void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                        def applicationSettings = new ApplicationSettingsDialogWrapper(project, coreClient)
                        applicationSettings.createCenterPanel()
                        applicationSettings.show()

                        if (applicationSettings.getOkayAction()) {
                            if (PluginBootstrap.getSourcePlugin() == null && SourcePluginConfig.current.appUuid != null) {
                                startSourcePlugin(coreClient)
                            }
                        }
                    }
                }))
    }

    private static void registerCodecs() {
        sourcePlugin.vertx.eventBus().registerDefaultCodec(IntelliJSourceFileMarker.class,
                SourceMessage.messageCodec(IntelliJSourceFileMarker.class))
        sourcePlugin.vertx.eventBus().registerDefaultCodec(IntelliJMethodGutterMark.class,
                SourceMessage.messageCodec(IntelliJMethodGutterMark.class))
    }

    static void coordinateSourceFileOpened(@NotNull SourcePlugin sourcePlugin, @NotNull PsiClassOwner psiFile) {
        if (psiFile.virtualFile.getUserData(IntelliJSourceFileMarker.KEY) != null || psiFile.project.isDisposed()) {
            return
        } else if (((PsiClassOwner) psiFile).getClasses().length == 0) {
            return
        }

        String className = ((PsiClassOwner) psiFile).getClasses().collect {
            it.qualifiedName
        }.toArray(new String[0])[0] //todo: better (this probably doesn't work with inner classes)

        def sourceFile = new PluginSourceFile(new File(psiFile.virtualFile.toString()), className)
        def fileMarker = new IntelliJSourceFileMarker(psiFile, sourceFile)
        psiFile.virtualFile.putUserData(IntelliJSourceFileMarker.KEY, fileMarker)

        //activate any source code markings
        sourcePlugin.activateSourceFileMarker(fileMarker)

        //display gutter mark tooltips on hover over gutter mark
        def editor = FileEditorManager.getInstance(psiFile.project).getSelectedTextEditor()
        if (editor) {
            editor.addEditorMouseMotionListener(editorMouseMotionListener = makeMouseMotionListener(
                    sourcePlugin.vertx, editor, psiFile))
        } else {
            log.error("Selected editor was null. Failed to add mouse motion listener")
        }
    }

    static EditorMouseMotionListener makeMouseMotionListener(Vertx vertx, Editor editor, PsiFile psiFile) {
        new EditorMouseMotionListener() {
            @Override
            synchronized void mouseMoved(EditorMouseEvent e) {
                if (e.area != EditorMouseEventArea.LINE_MARKERS_AREA) {
                    return
                } else if (e.isConsumed()) {
                    return
                }

                int lineNumber = IntelliUtils.convertPointToLineNumber(psiFile.project, e.mouseEvent.point)
                if (lineNumber == -1) {
                    return
                }

                def document = PsiDocumentManager.getInstance(psiFile.project).getCachedDocument(psiFile)
                if (document == null) {
                    return
                }
                def markupModel = DocumentMarkupModel.forDocument(document, psiFile.project, false)
                def gutterMark = getGutterMark(markupModel, lineNumber)
                if (gutterMark == null) {
                    return
                } else {
                    e.consume()
                }

                vertx.eventBus().send(TooltipViewTracker.CAN_OPEN_TOOLTIP,
                        gutterMark.sourceMethod.artifactQualifiedName(), {
                    if (it.result().body() == true) {
                        gutterMark.displayTooltip(vertx, editor, true)
                    }
                })
            }

            @Override
            synchronized void mouseDragged(EditorMouseEvent e) {
            }
        }
    }

    @Nullable
    private static IntelliJMethodGutterMark getGutterMark(MarkupModel markupModel, int lineNumber) {
        def highlighters = markupModel.getAllHighlighters()
        IntelliJMethodGutterMark rtnValue = null
        highlighters.each {
            if (it.gutterIconRenderer instanceof SourceArtifactLineMarkerGutterIconRenderer) {
                def gutterMark = ((SourceArtifactLineMarkerGutterIconRenderer) it.gutterIconRenderer).gutterMark
                if (gutterMark.lineNumber == lineNumber) {
                    rtnValue = gutterMark
                }
            }
        }
        return rtnValue
    }
}
