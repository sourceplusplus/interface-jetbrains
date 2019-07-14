package com.sourceplusplus.plugin.intellij

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFile
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginSourceFile
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.coordinate.artifact.track.FileClosedTracker
import com.sourceplusplus.plugin.intellij.inspect.IntelliJInspectionProvider
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import com.sourceplusplus.plugin.intellij.marker.mark.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.settings.application.ApplicationSettingsDialogWrapper
import com.sourceplusplus.plugin.intellij.settings.connect.EnvironmentDialogWrapper
import com.sourceplusplus.plugin.intellij.source.navigate.IntelliJArtifactNavigator
import com.sourceplusplus.plugin.intellij.tool.SourcePluginConsoleService
import com.sourceplusplus.plugin.intellij.util.IntelliUtils
import com.sourceplusplus.plugin.marker.mark.GutterMark
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import com.sourceplusplus.portal.display.PortalInterface
import io.vertx.core.Vertx
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.PatternLayout
import org.apache.log4j.spi.LoggingEvent
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import javax.swing.event.HyperlinkEvent
import java.awt.*
import java.util.concurrent.CountDownLatch

/**
 * todo: description
 *
 * @version 0.2.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJStartupActivity implements StartupActivity {

    //todo: fix https://github.com/sourceplusplus/Assistant/issues/1 and remove static block below
    static {
        ConsoleAppender console = new ConsoleAppender()
        console.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"))
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
        } else if (System.getProperty("os.name").toLowerCase().startsWith("linux")
                && ApplicationInfo.getInstance().majorVersion == "2019") {
            //https://github.com/sourceplusplus/Assistant/issues/68
            Notifications.Bus.notify(
                    new Notification("Source++", "Linux Unsupported",
                            "Source++ is currently unsupported on Linux. " +
                                    "For more information visit: <a href=\"#\">https://github.com/sourceplusplus/Assistant/issues/68</a>",
                            NotificationType.INFORMATION, new NotificationListener() {
                        @Override
                        void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                            try {
                                Desktop.getDesktop().browse(URI.create("https://github.com/sourceplusplus/Assistant/issues/68"))
                            } catch (Exception e) {
                                e.printStackTrace()
                            }
                        }
                    }))
            return
        }
        System.setProperty("vertx.disableFileCPResolving", "true")

        //redirect loggers to console
        def consoleView = ServiceManager.getService(project, SourcePluginConsoleService.class).getConsoleView()
        org.apache.log4j.Logger.getLogger("com.sourceplusplus").addAppender(new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent loggingEvent) {
                Object message = loggingEvent.message
                if (loggingEvent.level.isGreaterOrEqual(Level.WARN)) {
                    if (message.toString().startsWith("[PORTAL]")) {
                        consoleView.print(message.toString() + "\n", ConsoleViewContentType.ERROR_OUTPUT)
                    } else {
                        consoleView.print("[PLUGIN] - " + message.toString() + "\n", ConsoleViewContentType.ERROR_OUTPUT)
                    }
                } else if (loggingEvent.level.isGreaterOrEqual(Level.INFO)) {
                    if (message.toString().startsWith("[PORTAL]")) {
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

        if (SourcePluginConfig.current.activeEnvironment != null) {
            def coreClient = new SourceCoreClient(SourcePluginConfig.current.activeEnvironment.sppUrl)
            if (SourcePluginConfig.current.activeEnvironment.apiKey) {
                coreClient.apiKey = SourcePluginConfig.current.activeEnvironment.apiKey
            }

            coreClient.info({
                if (it.failed()) {
                    notifyNoConnection()
                } else {
                    SourcePluginConfig.current.activeEnvironment.coreClient = coreClient
                    if (SourcePluginConfig.current.activeEnvironment.appUuid == null) {
                        doApplicationSettingsDialog(project, coreClient)
                    } else {
                        coreClient.getApplication(SourcePluginConfig.current.activeEnvironment.appUuid, {
                            if (it.failed() || !it.result().isPresent()) {
                                SourcePluginConfig.current.activeEnvironment.appUuid = null
                                doApplicationSettingsDialog(project, coreClient)
                            } else {
                                startSourcePlugin(coreClient)
                            }
                        })
                    }
                }
            })
        } else {
            notifyNoConnection()
        }

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

    private static notifyNoConnection() {
        Notifications.Bus.notify(
                new Notification("Source++", "Connection Required",
                        "Source++ must be connected to a valid host to activate. Please <a href=\"#\">connect</a> here.",
                        NotificationType.INFORMATION, new NotificationListener() {
                    @Override
                    void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                        def connectDialog = new EnvironmentDialogWrapper(currentProject)
                        connectDialog.createCenterPanel()
                        connectDialog.show()

                        if (SourcePluginConfig.current.activeEnvironment) {
                            def coreClient = new SourceCoreClient(SourcePluginConfig.current.activeEnvironment.sppUrl)
                            if (SourcePluginConfig.current.activeEnvironment.apiKey) {
                                coreClient.apiKey = SourcePluginConfig.current.activeEnvironment.apiKey
                            }
                            coreClient.ping({
                                if (it.succeeded()) {
                                    if (SourcePluginConfig.current.activeEnvironment?.appUuid == null) {
                                        doApplicationSettingsDialog(currentProject, coreClient)
                                    } else {
                                        coreClient.getApplication(SourcePluginConfig.current.activeEnvironment.appUuid, {
                                            if (it.failed() || !it.result().isPresent()) {
                                                SourcePluginConfig.current.activeEnvironment.appUuid = null
                                                doApplicationSettingsDialog(currentProject, coreClient)
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
    }

    static void startSourcePlugin(SourceCoreClient coreClient) {
        //start plugin
        sourcePlugin = new SourcePlugin(Objects.requireNonNull(coreClient))
        registerCodecs()
        coreClient.registerIP()

        //register coordinators
        sourcePlugin.vertx.deployVerticle(new IntelliJArtifactNavigator())

        sourcePlugin.vertx.eventBus().consumer(PortalInterface.PORTAL_READY, {
            //set portal theme
            UIManager.addPropertyChangeListener({
                if (it.newValue instanceof IntelliJLaf) {
                    PortalInterface.updateTheme(false)
                } else {
                    PortalInterface.updateTheme(true)
                }
            })
            if (UIManager.lookAndFeel instanceof IntelliJLaf) {
                PortalInterface.updateTheme(false)
            } else {
                PortalInterface.updateTheme(true)
            }
        })
        DaemonCodeAnalyzerImpl.getInstance(currentProject).restart()
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

        def sourceFile = new PluginSourceFile(new File(psiFile.virtualFile.toString()),
                SourcePluginConfig.current.activeEnvironment.appUuid, className)
        def fileMarker = new IntelliJSourceFileMarker(psiFile, sourceFile)
        psiFile.virtualFile.putUserData(IntelliJSourceFileMarker.KEY, fileMarker)

        //activate any source code markings
        sourcePlugin.activateSourceFileMarker(fileMarker)

        //display gutter mark portals on hover over gutter mark
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

                def gutterMark
                def fileMarker = psiFile.virtualFile.getUserData(IntelliJSourceFileMarker.KEY)
                if (fileMarker != null) {
                    gutterMark = fileMarker.getSourceMarks().find {
                        (it as GutterMark).lineNumber == lineNumber && it.isViewable()
                    }
                }
                if (gutterMark == null) {
                    return
                } else {
                    e.consume()
                }

                vertx.eventBus().send(PortalViewTracker.CAN_OPEN_PORTAL,
                        gutterMark.sourceMethod.artifactQualifiedName(), {
                    if (it.result().body() == true) {
                        gutterMark.displayPortal(vertx, editor, true)
                    }
                })
            }

            @Override
            synchronized void mouseDragged(EditorMouseEvent e) {
            }
        }
    }
}
