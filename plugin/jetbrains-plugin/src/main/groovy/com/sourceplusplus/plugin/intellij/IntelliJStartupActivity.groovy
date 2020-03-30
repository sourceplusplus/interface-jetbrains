package com.sourceplusplus.plugin.intellij

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.intellij.marker.IntelliJSourceFileMarker
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJGutterMarkComponentProvider
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.settings.application.ApplicationSettingsDialogWrapper
import com.sourceplusplus.plugin.intellij.settings.connect.EnvironmentDialogWrapper
import com.sourceplusplus.plugin.intellij.tool.SourcePluginConsoleService
import com.sourceplusplus.portal.display.PortalInterface
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import org.apache.log4j.*
import org.apache.log4j.spi.LoggingEvent
import org.jetbrains.annotations.NotNull
import plus.sourceplus.marker.SourceFileMarker
import plus.sourceplus.marker.SourceFileMarkerProvider
import plus.sourceplus.marker.plugin.SourceMarkerPlugin
import plus.sourceplus.marker.plugin.SourceMarkerStartupActivity

import javax.swing.*
import javax.swing.event.HyperlinkEvent
import java.util.concurrent.CountDownLatch

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJStartupActivity extends SourceMarkerStartupActivity {

    //todo: fix https://github.com/sourceplusplus/Assistant/issues/1 and remove static block below
    static {
        ConsoleAppender console = new ConsoleAppender()
        console.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"))
        console.activateOptions()

        Logger.rootLogger.loggerRepository.resetConfiguration()
        Logger.getLogger("com.sourceplusplus").addAppender(console)
    }

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
        Logger.getLogger("com.sourceplusplus").addAppender(new AppenderSkeleton() {
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
        Disposer.register(project, consoleView)

        currentProject = project
        if (sourcePlugin != null) {
            CountDownLatch latch = new CountDownLatch(1)
            sourcePlugin.vertx.close({
                latch.countDown()
            })
            latch.await()
        }

        //load config
        def pluginConfig = PropertiesComponent.getInstance().getValue("spp_plugin_config")
        if (pluginConfig != null) {
            SourcePluginConfig.current.applyConfig(Json.decodeValue(pluginConfig, SourcePluginConfig.class))
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

        setupSourceMarker()
        super.runActivity(project)
        log.info("Source++ loaded for project: {} ({})", project.name, project.getPresentableUrl())
    }

    private static setupSourceMarker() {
        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration.componentProvider = new IntelliJGutterMarkComponentProvider()
        SourceMarkerPlugin.configuration.sourceFileMarkerProvider = new SourceFileMarkerProvider() {
            @Override
            SourceFileMarker createSourceFileMarker(@NotNull PsiFile psiFile) {
                return new IntelliJSourceFileMarker(psiFile)
            }
        }
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
        sourcePlugin.vertx.eventBus().registerDefaultCodec(IntelliJMethodGutterMark.class,
                SourceMessage.messageCodec(IntelliJMethodGutterMark.class))
    }
}
