package com.sourceplusplus.plugin.intellij

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
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
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.apache.commons.io.IOUtils
import org.jetbrains.annotations.NotNull

import javax.swing.*
import javax.swing.event.HyperlinkEvent
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch

/**
 * Starts up the plugin source marker.
 *
 * @version 0.3.1
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
        }
        System.setProperty("vertx.disableFileCPResolving", "true")
        Disposer.register(project, this)

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

            def coreServer = new SourceCoreServer(serverConfig, "embedded", null)
            SourcePlugin.vertx.deployVerticle(coreServer, {
                if (it.succeeded()) {
                    connectToEnvironment(coreServer.port)
                } else {
                    log.error("Failed to boot embedded Source++ Core", it.cause())
                }
            })
        } else {
            connectToEnvironment(-1)
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

    private static void connectToEnvironment(int embeddedServerPort) {
        if (SourcePluginConfig.current.activeEnvironment != null) {
            if (SourcePluginConfig.current.activeEnvironment.embedded) {
                if (SourcePluginConfig.current.embeddedCoreServer) {
                    SourcePluginConfig.current.activeEnvironment.apiPort = embeddedServerPort
                    SourcePluginConfig.current.getEnvironment(
                            SourcePluginConfig.current.activeEnvironment.environmentName).apiPort = embeddedServerPort
                } else {
                    //active environment is embedded but embedded mode is disable
                    SourcePluginConfig.current.activeEnvironment = null
                    notifyNoConnection()
                    return
                }
            }
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
                        determineSourceApplication()
                    } else {
                        coreClient.getApplication(SourcePluginConfig.current.activeEnvironment.appUuid, {
                            if (it.failed() || !it.result().isPresent()) {
                                SourcePluginConfig.current.activeEnvironment.appUuid = null
                                determineSourceApplication()
                            } else {
                                startSourcePlugin(coreClient)
                            }
                        })
                    }
                }
            })
        } else {
            if (embeddedServerPort != -1) {
                def coreClient = new SourceCoreClient("localhost", embeddedServerPort, false)
                coreClient.info({
                    if (it.succeeded()) {
                        saveAutoDetectedEnvironment("localhost", embeddedServerPort, "Embedded", true)
                    } else {
                        notifyNoConnection()
                    }
                })
            } else {
                //auto-detect local core
                def coreClient = new SourceCoreClient("localhost", 8080, false)
                coreClient.info({
                    if (it.succeeded()) {
                        saveAutoDetectedEnvironment("localhost", 8080, "Local", false)
                    } else {
                        //auto-detect docker core
                        coreClient = new SourceCoreClient("192.168.99.100", 8080, false)
                        coreClient.info({
                            if (it.succeeded()) {
                                saveAutoDetectedEnvironment("192.168.99.100", 8080, "Docker", false)
                            } else {
                                notifyNoConnection()
                            }
                        })
                    }
                })
            }
        }
    }

    private static void saveAutoDetectedEnvironment(String host, int port, String environmentName, boolean embedded) {
        def env = new SourceEnvironmentConfig()
        env.environmentName = environmentName
        env.apiHost = host
        env.apiPort = port
        env.embedded = embedded
        SourcePluginConfig.current.setEnvironments([env])
        SourcePluginConfig.current.activeEnvironment = env
        PropertiesComponent.getInstance().setValue("spp_plugin_config", Json.encode(SourcePluginConfig.current))
        determineSourceApplication()
    }

    private static notifyNoConnection() {
        Notifications.Bus.notify(new Notification("Source++", "Connection Required",
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
                                determineSourceApplication()
                            } else {
                                coreClient.getApplication(SourcePluginConfig.current.activeEnvironment.appUuid, {
                                    if (it.failed() || !it.result().isPresent()) {
                                        SourcePluginConfig.current.activeEnvironment.appUuid = null
                                        determineSourceApplication()
                                    } else {
                                        startSourcePlugin(coreClient)
                                    }
                                })
                            }
                        }
                    })
                }
            }
        }))
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

    private static void determineSourceApplication() {
        def coreClient = new SourceCoreClient(SourcePluginConfig.current.activeEnvironment.sppUrl)
        if (SourcePluginConfig.current.activeEnvironment.apiKey) {
            coreClient.apiKey = SourcePluginConfig.current.activeEnvironment.apiKey
        }

        ApplicationManager.getApplication().invokeLater {
            def applicationSettings = new ApplicationSettingsDialogWrapper(currentProject, coreClient)
            def appDomain = applicationSettings.applicationSettingsDialog.applicationDomain
            if (!appDomain.isEmpty()) {
                //create/get auto-detected application
                coreClient.getApplications({
                    if (it.succeeded()) {
                        def existingProject = it.result().find { it.appName() == currentProject.name }
                        if (!existingProject) {
                            coreClient.createApplication(SourceApplication.builder().isCreateRequest(true)
                                    .appName(currentProject.name).build(), {
                                if (it.succeeded()) {
                                    //todo: shouldn't need to double appUuid/applicationDomain up
                                    SourcePluginConfig.current.activeEnvironment.appUuid = it.result().appUuid()
                                    SourcePluginConfig.current.activeEnvironment.applicationDomain = appDomain
                                    SourcePluginConfig.current.getEnvironment(SourcePluginConfig.current.activeEnvironment.environmentName).appUuid = it.result().appUuid()
                                    SourcePluginConfig.current.getEnvironment(SourcePluginConfig.current.activeEnvironment.environmentName).applicationDomain = appDomain
                                    PropertiesComponent.getInstance().setValue("spp_plugin_config", Json.encode(SourcePluginConfig.current))
                                    startSourcePlugin(coreClient)
                                } else {
                                    log.error("Failed to create application", it.cause())
                                }
                            })
                        } else {
                            SourcePluginConfig.current.activeEnvironment.appUuid = existingProject.appUuid()
                            SourcePluginConfig.current.getEnvironment(SourcePluginConfig.current.activeEnvironment.environmentName).appUuid = existingProject.appUuid()
                            PropertiesComponent.getInstance().setValue("spp_plugin_config", Json.encode(SourcePluginConfig.current))
                            startSourcePlugin(coreClient)
                        }
                    } else {
                        log.error("Failed to get applications", it.cause())
                    }
                })
            } else {
                doApplicationSettingsDialog()
            }
        }
    }

    private static void doApplicationSettingsDialog() {
        Notifications.Bus.notify(new Notification("Source++", "Application Required",
                "Last step, Source++ also requires this project to be linked with a new or existing application. Please <a href=\"#\">choose</a> here.",
                NotificationType.INFORMATION, new NotificationListener() {
            @Override
            void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                def coreClient = new SourceCoreClient(SourcePluginConfig.current.activeEnvironment.sppUrl)
                if (SourcePluginConfig.current.activeEnvironment.apiKey) {
                    coreClient.apiKey = SourcePluginConfig.current.activeEnvironment.apiKey
                }
                def applicationSettings = new ApplicationSettingsDialogWrapper(currentProject, coreClient)
                applicationSettings.createCenterPanel()
                applicationSettings.show()
            }
        }))
    }

    private static void registerPluginCodecs() {
        SourceMessage.registerCodec(SourcePlugin.vertx, IntelliJMethodGutterMark.class)
    }
}
