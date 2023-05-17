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
package spp.jetbrains.sourcemarker

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import eu.geekplace.javapinning.JavaPinning
import eu.geekplace.javapinning.pin.Pin
import io.vertx.core.*
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.RequestOptions
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.net.TrustOptions
import io.vertx.kotlin.coroutines.await
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.ServiceDiscoveryOptions
import io.vertx.servicediscovery.impl.DiscoveryImpl
import io.vertx.serviceproxy.ServiceProxyBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import liveplugin.implementation.LivePluginProjectLoader
import org.apache.commons.text.CaseUtils
import spp.jetbrains.PluginBundle.message
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.UserData
import spp.jetbrains.insight.LiveInsightManager
import spp.jetbrains.marker.LanguageProvider
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.LivePluginService
import spp.jetbrains.marker.plugin.LiveStatusBarManager
import spp.jetbrains.marker.plugin.SourceInlayHintProvider
import spp.jetbrains.marker.plugin.SourceMarkerStartupActivity
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.command.status.LiveStatusBarManagerImpl
import spp.jetbrains.sourcemarker.config.SourceMarkerConfig
import spp.jetbrains.sourcemarker.config.getServicePortNormalized
import spp.jetbrains.sourcemarker.config.isSsl
import spp.jetbrains.sourcemarker.config.serviceHostNormalized
import spp.jetbrains.sourcemarker.discover.TCPServiceDiscoveryBackend
import spp.jetbrains.sourcemarker.instrument.InstrumentEventWindowService
import spp.jetbrains.sourcemarker.instrument.LiveInstrumentEventListener
import spp.jetbrains.sourcemarker.status.SourceStatusServiceImpl
import spp.jetbrains.sourcemarker.vcs.CodeChangeListener
import spp.jetbrains.sourcemarker.view.LiveViewChartManagerImpl
import spp.jetbrains.sourcemarker.view.LiveViewEventListener
import spp.jetbrains.sourcemarker.view.LiveViewLogManagerImpl
import spp.jetbrains.sourcemarker.view.LiveViewTraceManagerImpl
import spp.jetbrains.status.SourceStatus.*
import spp.jetbrains.status.SourceStatusListener
import spp.jetbrains.status.SourceStatusListener.Companion.TOPIC
import spp.jetbrains.status.SourceStatusService
import spp.protocol.service.LiveInstrumentService
import spp.protocol.service.LiveManagementService
import spp.protocol.service.LiveViewService
import spp.protocol.service.SourceServices
import java.io.File
import java.net.ConnectException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.SSLHandshakeException

/**
 * Sets up the SourceMarker plugin by configuring and initializing the various plugin modules.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceMarkerPlugin : SourceMarkerStartupActivity() {

    companion object {
        private const val SPP_PLUGIN_YML_PATH = ".spp/spp-plugin.yml"
        private val log = logger<SourceMarkerPlugin>()
        private val KEY = Key.create<SourceMarkerPlugin>("SPP_SOURCE_MARKER_PLUGIN")

        @Synchronized
        fun getInstance(project: Project): SourceMarkerPlugin {
            if (project.getUserData(KEY) == null) {
                val plugin = SourceMarkerPlugin()
                plugin.project = project
                project.putUserData(SourceStatusService.KEY, SourceStatusServiceImpl(project))
                project.putUserData(KEY, plugin)
            }
            return project.getUserData(KEY)!!
        }
    }

    private lateinit var project: Project
    private var loadLivePluginsLock = ReentrantLock()
    private var connectionJob: Job? = null
    private var discovery: ServiceDiscovery? = null
    private var addedConfigListener = false

    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return //tests manually set up necessary components
        }

        //make sure live view managers are initialized
        LiveViewChartManagerImpl.init(project)
        LiveViewTraceManagerImpl.init(project)
        LiveViewLogManagerImpl.init(project)

        //setup plugin
        safeRunBlocking { getInstance(project).init() }
        super.runActivity(project)
    }

    suspend fun init(configInput: SourceMarkerConfig? = null) {
        log.info("Initializing SourceMarkerPlugin on project: $project")
        disposePlugin()
        Disposer.register(project) {
            safeRunBlocking {
                try {
                    disposePlugin()
                } catch (e: Throwable) {
                    log.error("Error disposing plugin", e)
                }
            }
        }

        val options = if (System.getProperty("spp.debug.unblocked_threads", "false")!!.toBoolean()) {
            log.info("Removed blocked thread checker")
            VertxOptions().setBlockedThreadCheckInterval(Int.MAX_VALUE.toLong())
        } else {
            VertxOptions()
        }
        val vertx = UserData.vertx(project, Vertx.vertx(options))
        LivePluginProjectLoader.projectOpened(project)

        val config = configInput ?: getConfig()
        addSppPluginConfigChangeListener()

        connectionJob?.cancel()
        connectionJob = null

        connectionJob = vertx.safeLaunch {
            try {
                initServices(vertx, config)
                SourceStatusService.getInstance(project).start()

                if (!config.notifiedConnection) {
                    val pluginName = message("plugin_name")
                    Notifications.Bus.notify(
                        Notification(
                            message("plugin_name"), "Connection established",
                            "You have successfully connected. $pluginName is now fully activated.",
                            NotificationType.INFORMATION
                        ),
                        project
                    )
                    config.notifiedConnection = true

                    val projectSettings = PropertiesComponent.getInstance(project)
                    projectSettings.setValue("sourcemarker_plugin_config", Json.encode(config))
                }
            } catch (ignored: CancellationException) {
            } catch (throwable: Throwable) {
                SourceStatusService.getInstance(project).update(ConnectionError, throwable.message)
                log.warn("Connection failed", throwable)
                return@safeLaunch
            }

            val pluginsPromise = Promise.promise<Nothing>()
            ProgressManager.getInstance()
                .run(object : Task.Backgroundable(project, "Loading Source++ plugins", false, ALWAYS_BACKGROUND) {
                    override fun run(indicator: ProgressIndicator) {
                        if (loadLivePluginsLock.tryLock()) {
                            project.messageBus.connect().apply {
                                subscribe(TOPIC, SourceStatusListener {
                                    if (it == PluginsLoaded) {
                                        initMarker(vertx)
                                        disconnect()
                                    }
                                })
                            }

                            log.info("Loading live plugins for project: $project")
                            project.getUserData(LivePluginService.LIVE_PLUGIN_LOADER)!!.invoke()
                            log.info("Loaded live plugins for project: $project")
                            pluginsPromise.complete()
                            loadLivePluginsLock.unlock()
                        } else {
                            log.warn("Ignoring extraneous live plugins load request for project: $project")
                        }
                    }
                })
            pluginsPromise.future().await()
        }
    }

    private fun addSppPluginConfigChangeListener() {
        if (addedConfigListener) return
        addedConfigListener = true

        val localConfigListener = object : BulkFileListener {
            var lastUpdated = -1L
            override fun after(events: MutableList<out VFileEvent>) {
                val event = events.firstOrNull() as? VFileContentChangeEvent ?: return
                if (event.isFromSave && event.path.endsWith(SPP_PLUGIN_YML_PATH)) {
                    if (event.oldTimestamp <= lastUpdated) return else lastUpdated = event.oldTimestamp
                    DumbService.getInstance(project).smartInvokeLater {
                        val localConfig = loadSppPluginFileConfiguration()
                        if (localConfig != null && localConfig.override) {
                            log.info("Local config updated. Reloading plugin.")
                            safeRunBlocking { init(localConfig) }
                        }
                    }
                }
            }
        }
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, localConfigListener)
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

                val localConfig = try {
                    val objectMapper = ObjectMapper()
                    //ignore unknown properties (i.e old settings)
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    objectMapper.readValue(config.toString(), SourceMarkerConfig::class.java)
                } catch (ex: DecodeException) {
                    log.warn("Failed to decode $SPP_PLUGIN_YML_PATH", ex)
                    return null
                }

                //merge with persisted config
                val persistedConfig = getPersistedConfig(PropertiesComponent.getInstance(project))
                if (persistedConfig != null) {
                    localConfig.notifiedConnection = persistedConfig.notifiedConnection
                }
                return localConfig
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
                persistedConfig?.apply {
                    override = false
                } ?: SourceMarkerConfig()
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

    private suspend fun discoverAvailableServices(vertx: Vertx, config: SourceMarkerConfig) {
        SourceStatusService.getInstance(project).update(Pending, "Discovering available services")

        val originalClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = javaClass.classLoader
            discovery = DiscoveryImpl(
                vertx,
                ServiceDiscoveryOptions().setBackendConfiguration(
                    JsonObject()
                        .put("backend-name", "tcp-service-discovery")
                        .put("sourcemarker_plugin_config", JsonObject.mapFrom(config))
                        .put("project_location_hash", project.locationHash)
                )
            )
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }

        val liveStatusManager = LiveStatusBarManagerImpl(project, vertx)
        project.putUserData(LiveStatusBarManager.KEY, liveStatusManager)

        log.info("Discovering available services")
        val availableRecords = discovery!!.getRecords { true }.await()
        log.info("Discovered $availableRecords.size services")

        //live service
        if (availableRecords.any { it.name == SourceServices.LIVE_MANAGEMENT }) {
            log.info("Live management available")

            val liveManagementService = ServiceProxyBuilder(vertx)
                .apply { config.accessToken?.let { setToken(it) } }
                .setAddress(SourceServices.LIVE_MANAGEMENT)
                .build(LiveManagementService::class.java)
            UserData.liveManagementService(project, liveManagementService)

            //todo: selfInfo listener to trigger on changes
            log.info("Getting self info")
            val selfInfo = liveManagementService.getSelf().await()
            UserData.selfInfo(project, selfInfo)
            log.info("Self info: $selfInfo")
        } else {
            log.warn("Live management unavailable")
        }

        //live instrument
        if (availableRecords.any { it.name == SourceServices.LIVE_INSTRUMENT }) {
            log.info("Live instruments available")

            val liveInstrument = ServiceProxyBuilder(vertx)
                .apply { config.accessToken?.let { setToken(it) } }
                .setAddress(SourceServices.LIVE_INSTRUMENT)
                .build(LiveInstrumentService::class.java)
            UserData.liveInstrumentService(project, liveInstrument)

            ApplicationManager.getApplication().invokeLater {
                InstrumentEventWindowService.getInstance(project).makeOverviewTab()
            }
            val eventListener = LiveInstrumentEventListener(project, config)
            vertx.deployVerticle(eventListener).await()
            SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(eventListener)
        } else {
            log.warn("Live instruments unavailable")
        }

        //live view
        if (availableRecords.any { it.name == SourceServices.LIVE_VIEW }) {
            log.info("Live views available")
            val liveView = ServiceProxyBuilder(vertx)
                .apply { config.accessToken?.let { setToken(it) } }
                .setAddress(SourceServices.LIVE_VIEW)
                .build(LiveViewService::class.java)
            UserData.liveViewService(project, liveView)

            val eventListener = LiveViewEventListener(project, config)
            vertx.deployVerticle(eventListener).await()
        } else {
            log.warn("Live views unavailable")
        }

        //live insight
        val insightServiceAvailable = availableRecords.any { it.name == SourceServices.LIVE_INSIGHT }
        if (insightServiceAvailable || availableRecords.any { it.name == SourceServices.LIVE_VIEW }) {
            val insightManager = LiveInsightManager(insightServiceAvailable)
            vertx.deployVerticle(insightManager, DeploymentOptions().setWorker(true)).await()
            SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(insightManager)
        } else {
            log.warn("Live insights unavailable")
        }
    }

    suspend fun disposePlugin() {
        log.info("Disposing Source++ plugin. Project: ${project.name}")
        SourceMarker.getInstance(project).clearAvailableSourceFileMarkers()
        SourceMarker.getInstance(project).clearGlobalSourceMarkEventListeners()

        project.getUserData(LivePluginService.KEY)?.reset()

        TCPServiceDiscoveryBackend.closeSocket(project)
        discovery?.close()
        discovery = null

        if (UserData.hasVertx(project)) {
            UserData.vertx(project).close().await()
        }
        UserData.clear(project)
    }

    private suspend fun initServices(vertx: Vertx, config: SourceMarkerConfig) {
        SourceStatusService.getInstance(project).update(Pending, "Logging in")

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

            val tokenUri = "/api/new-token?authorization_code=" + config.authorizationCode
            val req = try {
                vertx.createHttpClient(httpClientOptions).request(
                    RequestOptions()
                        .setSsl(config.isSsl())
                        .setHost(config.serviceHostNormalized)
                        .setPort(config.getServicePortNormalized())
                        .setURI(tokenUri)
                ).await()
            } catch (ignore: ConnectException) {
                vertx.createHttpClient(httpClientOptions).request(
                    RequestOptions()
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
                    config.accessToken = body
                }

                discoverAvailableServices(vertx, config)
            } else {
                error("Error getting service token: ${resp.statusCode()} ${resp.statusMessage()}")
            }
        } else {
            //try default local access
            try {
                tryDefaultAccess(vertx, true, config)
            } catch (ignore: SSLHandshakeException) {
                tryDefaultAccess(vertx, false, config)
            } catch (e: Exception) {
                log.warn("Unable to find local live platform", e)
            }
        }
    }

    private suspend fun tryDefaultAccess(vertx: Vertx, ssl: Boolean, config: SourceMarkerConfig) {
        val defaultAuthorizationCode = "change-me"
        val tokenUri = "/api/new-token?authorization_code=$defaultAuthorizationCode"
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
                config.accessToken = body
            }

            if (ssl) {
                config.serviceHost = "https://localhost:" + SourceMarkerConfig.DEFAULT_SERVICE_PORT
            } else {
                config.serviceHost = "http://localhost:" + SourceMarkerConfig.DEFAULT_SERVICE_PORT
            }
            config.authorizationCode = defaultAuthorizationCode
            config.verifyHost = false

            val projectSettings = PropertiesComponent.getInstance(project)
            projectSettings.setValue("sourcemarker_plugin_config", Json.encode(config))

            discoverAvailableServices(vertx, config)

            //auto-established notification
            Notifications.Bus.notify(
                Notification(
                    message("plugin_name"), "Connection auto-established",
                    buildString {
                        append("You have successfully auto-connected to Live Platform. ")
                        append(message("plugin_name"))
                        append(" is now fully activated.")
                    },
                    NotificationType.INFORMATION
                ),
                project
            )

            config.notifiedConnection = true
            projectSettings.setValue("sourcemarker_plugin_config", Json.encode(config))
        }
    }

    private fun initMarker(vertx: Vertx) {
        log.info("Initializing marker")
        val originalClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = javaClass.classLoader
            ServiceLoader.load(LanguageProvider::class.java).forEach {
                if (it.canSetup()) it.setup(project)
            }
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }

        vertx.deployVerticle(CodeChangeListener(project)).onFailure {
            log.error("Unable to deploy code change listener", it)
        }

        SourceMarker.getInstance(project).apply {
            addGlobalSourceMarkEventListener(SourceInlayHintProvider.EVENT_LISTENER)
        }

        //force marker re-processing
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
