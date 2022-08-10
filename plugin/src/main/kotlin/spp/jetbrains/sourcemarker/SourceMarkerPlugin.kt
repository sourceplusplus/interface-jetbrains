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
package spp.jetbrains.sourcemarker

import com.apollographql.apollo3.exception.ApolloHttpException
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import eu.geekplace.javapinning.JavaPinning
import eu.geekplace.javapinning.pin.Pin
import io.vertx.core.MultiMap
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.RequestOptions
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.net.TrustOptions
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.ServiceDiscoveryOptions
import io.vertx.servicediscovery.impl.DiscoveryImpl
import io.vertx.serviceproxy.ServiceProxyBuilder
import kotlinx.coroutines.*
import liveplugin.implementation.LivePluginProjectLoader
import liveplugin.implementation.plugin.LivePluginService
import org.apache.commons.text.CaseUtils
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.ArtifactSearch
import spp.jetbrains.marker.jvm.JVMMarker
import spp.jetbrains.marker.plugin.SourceInlayHintProvider
import spp.jetbrains.marker.py.PythonMarker
import spp.jetbrains.marker.source.mark.api.filter.CreateSourceMarkFilter
import spp.jetbrains.monitor.skywalking.SkywalkingMonitor
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity.Companion.INTELLIJ_PRODUCT_CODES
import spp.jetbrains.sourcemarker.command.ControlBarController
import spp.jetbrains.sourcemarker.mark.PluginSourceMarkEventListener
import spp.jetbrains.sourcemarker.portal.PortalController
import spp.jetbrains.sourcemarker.service.LiveInstrumentManager
import spp.jetbrains.sourcemarker.service.LiveViewManager
import spp.jetbrains.sourcemarker.service.discover.TCPServiceDiscoveryBackend
import spp.jetbrains.sourcemarker.service.instrument.breakpoint.BreakpointHitWindowService
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.jetbrains.sourcemarker.settings.getServicePortNormalized
import spp.jetbrains.sourcemarker.settings.isSsl
import spp.jetbrains.sourcemarker.settings.serviceHostNormalized
import spp.jetbrains.sourcemarker.status.LiveStatusManager
import spp.protocol.SourceServices
import spp.protocol.SourceServices.Instance
import spp.protocol.service.LiveInstrumentService
import spp.protocol.service.LiveService
import spp.protocol.service.LiveViewService
import java.io.File
import java.net.ConnectException
import javax.net.ssl.SSLHandshakeException

/**
 * Sets up the SourceMarker plugin by configuring and initializing the various plugin modules.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class SourceMarkerPlugin(val project: Project) {

    companion object {
        private const val SPP_PLUGIN_YML_PATH = ".spp/spp-plugin.yml"
        private val log = logger<SourceMarkerPlugin>()
        private val KEY = Key.create<SourceMarkerPlugin>("SPP_SOURCE_MARKER_PLUGIN")

        @Synchronized
        fun getInstance(project: Project): SourceMarkerPlugin {
            if (project.getUserData(KEY) == null) {
                val sourceMarkerPlugin = SourceMarkerPlugin(project)
                project.putUserData(KEY, sourceMarkerPlugin)
                project.putUserData(SourceMarker.VERTX_KEY, sourceMarkerPlugin.vertx)
            }
            return project.getUserData(KEY)!!
        }
    }

    private val deploymentIds = mutableListOf<String>()
    private var connectionJob: Job? = null
    private var discovery: ServiceDiscovery? = null
    private var addedConfigListener = false
    val vertx: Vertx

    init {
        SourceMarker.getInstance(project).enabled = false
        val options = if (System.getProperty("sourcemarker.debug.unblocked_threads", "false")!!.toBoolean()) {
            log.info("Removed blocked thread checker")
            VertxOptions().setBlockedThreadCheckInterval(Int.MAX_VALUE.toLong())
        } else {
            VertxOptions()
        }
        vertx = Vertx.vertx(options)

        if (JVMMarker.canSetup()) {
            log.info("Setting up JVM marker")
            JVMMarker.setup()
        }
        if (PythonMarker.canSetup()) {
            log.info("Setting up Python marker")
            PythonMarker.setup()
        }
    }

    suspend fun init(
        configInput: SourceMarkerConfig? = null,
        notifySuccessfulConnection: Boolean = false
    ) {
        log.info("Initializing SourceMarkerPlugin on project: $project")
        restartIfNecessary()
        LivePluginProjectLoader.projectClosing(project)
        LivePluginProjectLoader.projectOpened(project)

        val config = configInput ?: getConfig()
        if (!addedConfigListener) {
            addedConfigListener = true
            val localConfigListener = object : BulkFileListener {
                var lastUpdated = -1L
                override fun after(events: MutableList<out VFileEvent>) {
                    val event = events.firstOrNull() ?: return
                    if (event is VFileContentChangeEvent && event.isFromSave && event.path.endsWith(SPP_PLUGIN_YML_PATH)) {
                        if (event.oldTimestamp <= lastUpdated) return else lastUpdated = event.oldTimestamp
                        DumbService.getInstance(project).smartInvokeLater {
                            val localConfig = loadSppPluginFileConfiguration()
                            if (localConfig != null && localConfig.override) {
                                runBlocking {
                                    init(localConfig, true)
                                }
                            }
                        }
                    }
                }
            }
            project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, localConfigListener)
        }

        //attempt to determine root source package automatically (if necessary)
        if (config.rootSourcePackages.isEmpty()) {
            if (INTELLIJ_PRODUCT_CODES.contains(ApplicationInfo.getInstance().build.productCode)) {
                val rootPackage = ArtifactSearch.detectRootPackage(project)
                if (rootPackage != null) {
                    log.info("Detected root source package: $rootPackage")
                    config.rootSourcePackages = listOf(rootPackage)
                    PropertiesComponent.getInstance(project)
                        .setValue("sourcemarker_plugin_config", Json.encode(config))
                }
            }
        }

        connectionJob?.cancel()
        connectionJob = null

        connectionJob = GlobalScope.launch(vertx.dispatcher()) {
            var connectedMonitor = false
            try {
                initServices(config)
                initMonitor(config)
                connectedMonitor = true

                if (notifySuccessfulConnection) {
                    val pluginName = message("plugin_name")
                    Notifications.Bus.notify(
                        Notification(
                            message("plugin_name"), "Connection established",
                            "You have successfully connected. $pluginName is now fully activated.",
                            NotificationType.INFORMATION
                        )
                    )
                }
            } catch (ignored: CancellationException) {
            } catch (throwable: ApolloHttpException) {
                val pluginName = message("plugin_name")
                if (throwable.statusCode == 401) {
                    Notifications.Bus.notify(
                        Notification(
                            pluginName, "Connection unauthorized",
                            "Failed to authenticate with $pluginName. " +
                                    "Please ensure the correct configuration " +
                                    "is set at: Settings -> Tools -> $pluginName",
                            NotificationType.ERROR
                        )
                    )
                } else {
                    Notifications.Bus.notify(
                        Notification(
                            pluginName, "Connection failed",
                            "Failed to connect to $pluginName. " +
                                    "Please ensure the correct configuration " +
                                    "is set at: Settings -> Tools -> $pluginName",
                            NotificationType.ERROR
                        )
                    )
                }
            } catch (throwable: Throwable) {
                //todo: if first time bring up config panel automatically instead of notification
                val pluginName = message("plugin_name")
                if (throwable.message == "HTTP 401 Unauthorized") {
                    Notifications.Bus.notify(
                        Notification(
                            pluginName, "Connection unauthorized",
                            "Failed to authenticate with $pluginName. " +
                                    "Please ensure the correct configuration " +
                                    "is set at: Settings -> Tools -> $pluginName",
                            NotificationType.ERROR
                        )
                    )
                } else if (throwable.message == "Failed to create SSL connection") {
                    Notifications.Bus.notify(
                        Notification(
                            pluginName, "SSL connection failed",
                            "Failed to create SSL connection to $pluginName. " +
                                    "Please ensure the correct configuration " +
                                    "is set at: Settings -> Tools -> $pluginName",
                            NotificationType.ERROR
                        )
                    )
                } else {
                    Notifications.Bus.notify(
                        Notification(
                            pluginName, "Connection failed",
                            "$pluginName failed to connect to Apache SkyWalking. " +
                                    "Please ensure Apache SkyWalking is running and the correct configuration " +
                                    "is set at: Settings -> Tools -> $pluginName",
                            NotificationType.ERROR
                        )
                    )
                }
                log.error("Connection failed", throwable)
            }

            if (connectedMonitor) {
                initUI(config)

                val pluginsPromise = Promise.promise<Nothing>()
                ProgressManager.getInstance()
                    .run(object : Task.Backgroundable(project, "Loading Source++ plugins", false, ALWAYS_BACKGROUND) {
                        override fun run(indicator: ProgressIndicator) {
                            log.info("Loading live plugins")
                            project.getUserData(LivePluginService.LIVE_PLUGIN_LOADER)!!.invoke()
                            log.info("Loaded live plugins")
                            pluginsPromise.complete()
                        }
                    })
                pluginsPromise.future().await()
                initMarker(config)
            }
        }
    }

    fun loadSppPluginFileConfiguration(): SourceMarkerConfig? {
        if (project.basePath != null) {
            val configFile = File(project.basePath, SPP_PLUGIN_YML_PATH)
            if (configFile.exists()) {
                var config = try {
                    JsonObject(
                        ObjectMapper().writeValueAsString(YAMLMapper().readValue(configFile, Object::class.java))
                    )
                } catch (ex: JacksonException) {
                    log.error("Failed to parse config file ${configFile.absolutePath}", ex)
                    return null
                }

                val commandConfig = config.remove("command_config")
                config = convertConfigToCamelCase(config)
                config.put("commandConfig", commandConfig)

                return try {
                    val objectMapper = ObjectMapper()
                    //ignore unknown properties (i.e old settings)
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    objectMapper.readValue(config.toString(), SourceMarkerConfig::class.java)
                } catch (ex: DecodeException) {
                    log.warn("Failed to decode $SPP_PLUGIN_YML_PATH", ex)
                    return null
                }
            }
        }
        return null
    }

    private fun convertConfigToCamelCase(jsonObject: JsonObject): JsonObject {
        val result = JsonObject(jsonObject.toString())
        result.fieldNames().toList().forEach {
            val value = result.remove(it)
            if (value is JsonObject) {
                result.put(CaseUtils.toCamelCase(it, false, '_'), convertConfigToCamelCase(value))
            } else {
                result.put(CaseUtils.toCamelCase(it, false, '_'), value)
            }
        }
        return result
    }

    fun getConfig(): SourceMarkerConfig {
        val fileConfig = loadSppPluginFileConfiguration()
        val config = if (fileConfig != null && fileConfig.override) {
            fileConfig
        } else {
            val persistedConfig = getPersistedConfig(PropertiesComponent.getInstance(project))
            if (persistedConfig == null && fileConfig != null) {
                fileConfig
            } else {
                persistedConfig ?: SourceMarkerConfig()
            }
        }
        return config
    }

    private fun getPersistedConfig(projectSettings: PropertiesComponent): SourceMarkerConfig? {
        if (projectSettings.isValueSet("sourcemarker_plugin_config")) {
            try {
                return Json.decodeValue(
                    projectSettings.getValue("sourcemarker_plugin_config"),
                    SourceMarkerConfig::class.java
                )
            } catch (ex: DecodeException) {
                log.warn("Failed to decode SourceMarker configuration", ex)
                projectSettings.unsetValue("sourcemarker_plugin_config")
            }
        }
        return null
    }

    private suspend fun discoverAvailableServices(config: SourceMarkerConfig) {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = javaClass.classLoader
            discovery = DiscoveryImpl(
                vertx,
                ServiceDiscoveryOptions().setBackendConfiguration(
                    JsonObject()
                        .put("backend-name", "tcp-service-discovery")
                        .put("sourcemarker_plugin_config", JsonObject.mapFrom(config))
                )
            )
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }

        log.info("Discovering available services")
        val availableRecords = discovery!!.getRecords { true }.await()
        log.info("Discovered $availableRecords.size services")

        //live service
        if (availableRecords.any { it.name == SourceServices.Utilize.LIVE_SERVICE }) {
            log.info("Live service available")

            Instance.liveService = ServiceProxyBuilder(vertx)
                .apply { config.serviceToken?.let { setToken(it) } }
                .setAddress(SourceServices.Utilize.LIVE_SERVICE)
                .build(LiveService::class.java)
            project.putUserData(SkywalkingMonitor.LIVE_SERVICE, Instance.liveService)
        } else {
            log.warn("Live service unavailable")
        }

        //live instrument
        if (availableRecords.any { it.name == SourceServices.Utilize.LIVE_INSTRUMENT }) {
            log.info("Live instruments available")
            SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(LiveStatusManager)

            Instance.liveInstrument = ServiceProxyBuilder(vertx)
                .apply { config.serviceToken?.let { setToken(it) } }
                .setAddress(SourceServices.Utilize.LIVE_INSTRUMENT)
                .build(LiveInstrumentService::class.java)
            project.putUserData(SkywalkingMonitor.LIVE_INSTRUMENT_SERVICE, Instance.liveInstrument)

            ApplicationManager.getApplication().invokeLater {
                BreakpointHitWindowService.getInstance(project).showEventsWindow()
            }
            val breakpointListener = LiveInstrumentManager(project, config)
            GlobalScope.launch(vertx.dispatcher()) {
                deploymentIds.add(vertx.deployVerticle(breakpointListener).await())
            }
        } else {
            log.warn("Live instruments unavailable")
        }

        //live view
        if (availableRecords.any { it.name == SourceServices.Utilize.LIVE_VIEW }) {
            log.info("Live views available")
            Instance.liveView = ServiceProxyBuilder(vertx)
                .apply { config.serviceToken?.let { setToken(it) } }
                .setAddress(SourceServices.Utilize.LIVE_VIEW)
                .build(LiveViewService::class.java)
            project.putUserData(SkywalkingMonitor.LIVE_VIEW_SERVICE, Instance.liveView)

            val viewListener = LiveViewManager(project, config)
            GlobalScope.launch(vertx.dispatcher()) {
                deploymentIds.add(vertx.deployVerticle(viewListener).await())
            }
        } else {
            log.warn("Live views unavailable")
        }
    }

    private suspend fun restartIfNecessary() {
        if (SourceMarker.getInstance(project).enabled) {
            SourceMarker.getInstance(project).clearAvailableSourceFileMarkers()
            SourceMarker.getInstance(project).clearGlobalSourceMarkEventListeners()
        }
        SourceMarker.getInstance(project).enabled = false

        deploymentIds.forEach { vertx.undeploy(it).await() }
        deploymentIds.clear()

        TCPServiceDiscoveryBackend.socket?.close()?.await()
        TCPServiceDiscoveryBackend.socket = null
        discovery?.close()
        discovery = null

        Instance.clearServices()
        ControlBarController.clearAvailableCommands()
    }

    private suspend fun initServices(config: SourceMarkerConfig) {
        if (!config.serviceHost.isNullOrBlank()) {
            val certificatePins = mutableListOf<String>()
            certificatePins.addAll(config.certificatePins)
            val httpClientOptions = if (certificatePins.isNotEmpty()) {
                HttpClientOptions()
                    .setTrustOptions(
                        TrustOptions.wrap(
                            JavaPinning.trustManagerForPins(certificatePins.map { Pin.fromString("CERTSHA256:$it") })
                        )
                    )
                    .setVerifyHost(false)
            } else {
                HttpClientOptions().apply {
                    if (config.isSsl()) {
                        isSsl = config.isSsl()
                        isVerifyHost = false
                        isTrustAll = true
                    }
                }
            }

            val tokenUri = "/api/new-token?access_token=" + config.accessToken
            val req = try {
                vertx.createHttpClient(httpClientOptions).request(
                    RequestOptions()
                        .setHeaders(MultiMap.caseInsensitiveMultiMap().add("spp-platform-request", "true"))
                        .setSsl(config.isSsl())
                        .setHost(config.serviceHostNormalized)
                        .setPort(config.getServicePortNormalized())
                        .setURI(tokenUri)
                ).await()
            } catch (ignore: ConnectException) {
                vertx.createHttpClient(httpClientOptions).request(
                    RequestOptions()
                        .setHeaders(MultiMap.caseInsensitiveMultiMap().add("spp-platform-request", "true"))
                        .setSsl(config.isSsl())
                        .setHost(config.serviceHostNormalized)
                        .setPort(config.isSsl().let { if (it) 443 else 80 }) //try default HTTP ports
                        .setURI(tokenUri)
                ).await().apply {
                    //update config with successful port
                    config.serviceHost = if (config.isSsl()) {
                        "https://${config.serviceHostNormalized}:443"
                    } else {
                        "http://${config.serviceHostNormalized}:80"
                    }
                }
            }
            req.end().await()
            val resp = req.response().await()
            if (resp.statusCode() in 200..299) {
                val body = resp.body().await().toString()
                if (resp.statusCode() != 202) {
                    config.serviceToken = body
                }

                discoverAvailableServices(config)
            } else {
                config.serviceToken = null
            }
        } else {
            //try default local access
            try {
                tryDefaultAccess(true, config)
            } catch (ignore: SSLHandshakeException) {
                tryDefaultAccess(false, config)
            } catch (e: Exception) {
                log.warn("Unable to find local live platform", e)
            }
        }
    }

    private suspend fun tryDefaultAccess(ssl: Boolean, config: SourceMarkerConfig) {
        val defaultAccessToken = "change-me"
        val tokenUri = "/api/new-token?access_token=$defaultAccessToken"
        val req = vertx.createHttpClient(HttpClientOptions().setSsl(ssl).setVerifyHost(false).setTrustAll(true))
            .request(
                RequestOptions()
                    .setHeaders(MultiMap.caseInsensitiveMultiMap().add("spp-platform-request", "true"))
                    .setHost("localhost")
                    .setPort(SourceMarkerConfig.DEFAULT_SERVICE_PORT)
                    .setURI(tokenUri)
            ).await()
        req.end().await()

        val resp = req.response().await()
        if (resp.statusCode() in 200..299) {
            if (resp.statusCode() != 202) {
                val body = resp.body().await().toString()
                config.serviceToken = body
            }

            if (ssl) {
                config.serviceHost = "https://localhost:" + SourceMarkerConfig.DEFAULT_SERVICE_PORT
            } else {
                config.serviceHost = "http://localhost:" + SourceMarkerConfig.DEFAULT_SERVICE_PORT
            }
            config.accessToken = defaultAccessToken
            config.verifyHost = false

            val projectSettings = PropertiesComponent.getInstance(project)
            projectSettings.setValue("sourcemarker_plugin_config", Json.encode(config))

            discoverAvailableServices(config)

            //auto-established notification
            Notifications.Bus.notify(
                Notification(
                    message("plugin_name"), "Connection auto-established",
                    "You have successfully auto-connected to Live Platform. ${message("plugin_name")} is now fully activated.",
                    NotificationType.INFORMATION
                )
            )
        } else if (resp.statusCode() == 405) {
            //found skywalking OAP server
            if (ssl) {
                config.serviceHost = "https://localhost:" + SourceMarkerConfig.DEFAULT_SERVICE_PORT
            } else {
                config.serviceHost = "http://localhost:" + SourceMarkerConfig.DEFAULT_SERVICE_PORT
                config.verifyHost = false
            }
            val projectSettings = PropertiesComponent.getInstance(project)
            projectSettings.setValue("sourcemarker_plugin_config", Json.encode(config))

            //auto-established notification
            Notifications.Bus.notify(
                Notification(
                    message("plugin_name"), "Connection auto-established",
                    "You have successfully auto-connected to Apache SkyWalking. ${message("plugin_name")} is now fully activated.",
                    NotificationType.INFORMATION
                )
            )
        }
    }

    private suspend fun initMonitor(config: SourceMarkerConfig) {
        val scheme = if (config.isSsl()) "https" else "http"
        val skywalkingHost = "$scheme://${config.serviceHostNormalized}:${config.getServicePortNormalized()}/graphql"
        val certificatePins = mutableListOf<String>()
        certificatePins.addAll(config.certificatePins)
        deploymentIds.add(
            vertx.deployVerticle(
                SkywalkingMonitor(
                    skywalkingHost, config.serviceToken, certificatePins, config.verifyHost, config.serviceName, project
                )
            ).await()
        )
    }

    private suspend fun initUI(config: SourceMarkerConfig) {
        deploymentIds.add(vertx.deployVerticle(PortalController(project, config)).await())
    }

    private fun initMarker(config: SourceMarkerConfig) {
        log.info("Initializing marker")
        SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(SourceInlayHintProvider.EVENT_LISTENER)
        SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(PluginSourceMarkEventListener(project))

        SourceMarker.getInstance(project).configuration.guideMarkConfiguration.activateOnKeyboardShortcut = true
        SourceMarker.getInstance(project).configuration.inlayMarkConfiguration.strictlyManualCreation = true

        if (config.rootSourcePackages.isNotEmpty()) {
            SourceMarker.getInstance(project).configuration.createSourceMarkFilter = CreateSourceMarkFilter { artifactQualifiedName ->
                config.rootSourcePackages.any { artifactQualifiedName.identifier.startsWith(it) }
            }
        } else {
            val productCode = ApplicationInfo.getInstance().build.productCode
            if (INTELLIJ_PRODUCT_CODES.contains(productCode)) {
                log.warn("Could not determine root source package. Skipped adding create source mark filter...")
            }
        }
        SourceMarker.getInstance(project).enabled = true
        log.info("Source marker enabled")

        //force marker re-processing
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
