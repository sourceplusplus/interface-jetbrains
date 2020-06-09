package com.sourceplusplus.plugin.intellij

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.config.SourceEnvironmentConfig
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.core.SourceCoreServer
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.plugin.SourceMarkerStartupActivity
import com.sourceplusplus.plugin.SourcePlugin
import com.sourceplusplus.plugin.intellij.marker.mark.gutter.IntelliJMethodGutterMark
import com.sourceplusplus.plugin.intellij.portal.IntelliJPortalUI
import com.sourceplusplus.plugin.intellij.settings.application.ApplicationSettingsDialogWrapper
import com.sourceplusplus.plugin.intellij.settings.connect.EnvironmentDialogWrapper
import com.sourceplusplus.plugin.intellij.tool.SourcePluginConsoleService
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.apache.commons.io.IOUtils
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import org.jetbrains.annotations.NotNull

import javax.swing.*
import javax.swing.event.HyperlinkEvent
import java.awt.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch

/**
 * Starts up the plugin source marker.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class IntelliJStartupActivity extends SourceMarkerStartupActivity implements Disposable {

    private static SourcePlugin sourcePlugin
    public static Project currentProject

    @Override
    void runActivity(@NotNull Project project) {
        SourceMarkerPlugin.INSTANCE.enabled = false
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return //don't need to boot everything for unit tests
        } else if (System.getProperty("ide.browser.jcef.enabled") != "true") {
            Notifications.Bus.notify(
                    new Notification("Source++", "JCEF Disabled",
                            "Source++ requires JCEF enabled on IntelliJ IDEA. " +
                                    "For more information visit: <a href=\"#\">https://youtrack.jetbrains.com/issue/IDEA-231833#focus=streamItem-27-3941624.0-0</a>",
                            NotificationType.INFORMATION, new NotificationListener() {
                        @Override
                        void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                            try {
                                Desktop.getDesktop().browse(URI.create("https://youtrack.jetbrains.com/issue/IDEA-231833#focus=streamItem-27-3941624.0-0"))
                            } catch (Exception e) {
                                e.printStackTrace()
                            }
                        }
                    }))
            return
        }
        System.setProperty("vertx.disableFileCPResolving", "true")
        Disposer.register(project, this)

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
                        def module = loggingEvent.logger.getName().replace("com.sourceplusplus.", "")
                        module = module.substring(0, module.indexOf(".")).toUpperCase()
                        consoleView.print("[$module] - " + message.toString() + "\n", ConsoleViewContentType.ERROR_OUTPUT)
                    }
                } else if (loggingEvent.level.isGreaterOrEqual(Level.INFO)) {
                    if (message.toString().startsWith("[PORTAL]")) {
                        consoleView.print(message.toString() + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    } else {
                        def module = loggingEvent.logger.getName().replace("com.sourceplusplus.", "")
                        module = module.substring(0, module.indexOf(".")).toUpperCase()
                        consoleView.print("[$module] - " + message.toString() + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
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

        log.info("Loading Source++ for project: {} ({})", project.name, project.getPresentableUrl())
        currentProject = project
        if (sourcePlugin != null) {
            CountDownLatch latch = new CountDownLatch(1)
            SourcePlugin.vertx.close({
                latch.countDown()
            })
            latch.await()
        }

        //load config
        def pluginConfig = PropertiesComponent.getInstance().getValue("spp_plugin_config")
        if (pluginConfig != null) {
            SourcePluginConfig.current.applyConfig(Json.decodeValue(pluginConfig, SourcePluginConfig.class))
        }

        if (SourcePluginConfig.current.embeddedCoreServer) {
            log.info("Booting embedded Source++ Core")
            def configInputStream = IntelliJStartupActivity.class.getResourceAsStream("/config/embedded-core.json")
            def configData = IOUtils.toString(configInputStream, StandardCharsets.UTF_8)
            def serverConfig = new JsonObject(configData)

            SourcePlugin.vertx.deployVerticle(new SourceCoreServer(serverConfig, "embedded", null), {
                if (it.succeeded()) {
                    connectToEnvironment(true)
                } else {
                    log.error("Failed to boot embedded Source++ Core", it.cause())
                }
            })
        } else {
            connectToEnvironment(false)
        }

        super.runActivity(project)
    }

    @Override
    void dispose() {
        if (SourceMarkerPlugin.INSTANCE.enabled) {
            SourceMarkerPlugin.INSTANCE.clearAvailableSourceFileMarkers()
            SourcePlugin.vertx.close()
        }
    }

    private static void connectToEnvironment(boolean embeddedBooted) {
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
        } else {
            //auto-detect local core
            def coreClient = new SourceCoreClient("localhost", 8080, false)
            coreClient.info({
                if (it.succeeded()) {
                    saveAutoDetectedEnvironment(coreClient, "localhost", embeddedBooted ? "Embedded" : "Local")
                } else {
                    //auto-detect docker core
                    coreClient = new SourceCoreClient("192.168.99.100", 8080, false)
                    coreClient.info({
                        if (it.succeeded()) {
                            saveAutoDetectedEnvironment(coreClient, "192.168.99.100", "Docker")
                        } else {
                            notifyNoConnection()
                        }
                    })
                }
            })
        }
    }

    private static void saveAutoDetectedEnvironment(SourceCoreClient coreClient, String host, String environmentName) {
        def env = new SourceEnvironmentConfig()
        env.environmentName = environmentName
        env.apiHost = host
        env.apiPort = 8080
        SourcePluginConfig.current.setEnvironments([env])
        SourcePluginConfig.current.activeEnvironment = env
        PropertiesComponent.getInstance().setValue(
                "spp_plugin_config", Json.encode(SourcePluginConfig.current))
        doApplicationSettingsDialog(currentProject, coreClient)
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
        registerPluginCodecs()
        coreClient.registerIP()

        //register coordinators
        SourcePlugin.vertx.deployVerticle(new IntelliJArtifactNavigator())

        SourcePlugin.vertx.eventBus().consumer(IntelliJPortalUI.PORTAL_READY, {
            //set portal theme
            UIManager.addPropertyChangeListener({
                if (it.newValue instanceof IntelliJLaf) {
                    IntelliJPortalUI.updateTheme(false)
                } else {
                    IntelliJPortalUI.updateTheme(true)
                }
            })
            if (UIManager.lookAndFeel instanceof IntelliJLaf) {
                IntelliJPortalUI.updateTheme(false)
            } else {
                IntelliJPortalUI.updateTheme(true)
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

    private static void registerPluginCodecs() {
        SourceMessage.registerCodec(SourcePlugin.vertx, IntelliJMethodGutterMark.class)
    }
}
