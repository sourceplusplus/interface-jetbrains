package com.sourceplusplus.sourcemarker

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.ProjectScope
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.mark.api.component.api.config.ComponentSizeEvaluator
import com.sourceplusplus.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkSingleJcefComponentProvider
import com.sourceplusplus.marker.source.mark.api.filter.CreateSourceMarkFilter
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.monitor.skywalking.SkywalkingMonitor
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.backend.PortalServer
import com.sourceplusplus.protocol.SourceMarkerServices
import com.sourceplusplus.protocol.SourceMarkerServices.Instance
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.endpoint.EndpointResult
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.protocol.artifact.log.LogResult
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult
import com.sourceplusplus.protocol.artifact.trace.TraceStack
import com.sourceplusplus.protocol.service.live.LiveInstrumentService
import com.sourceplusplus.protocol.service.live.LiveViewService
import com.sourceplusplus.protocol.service.logging.LogCountIndicatorService
import com.sourceplusplus.protocol.service.tracing.LocalTracingService
import com.sourceplusplus.sourcemarker.PluginBundle.message
import com.sourceplusplus.sourcemarker.discover.TCPServiceDiscoveryBackend
import com.sourceplusplus.sourcemarker.listeners.PluginSourceMarkEventListener
import com.sourceplusplus.sourcemarker.listeners.PortalEventListener
import com.sourceplusplus.sourcemarker.service.LiveInstrumentManager
import com.sourceplusplus.sourcemarker.service.LiveViewManager
import com.sourceplusplus.sourcemarker.service.LogCountIndicators
import com.sourceplusplus.sourcemarker.service.breakpoint.BreakpointHitWindowService
import com.sourceplusplus.sourcemarker.settings.SourceMarkerConfig
import com.sourceplusplus.sourcemarker.settings.getServicePortNormalized
import com.sourceplusplus.sourcemarker.settings.isSsl
import com.sourceplusplus.sourcemarker.settings.serviceHostNormalized
import com.sourceplusplus.sourcemarker.status.LiveLogStatusManager
import eu.geekplace.javapinning.JavaPinning
import eu.geekplace.javapinning.pin.Pin
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.RequestOptions
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.net.TrustOptions
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.ServiceDiscoveryOptions
import io.vertx.servicediscovery.impl.DiscoveryImpl
import io.vertx.serviceproxy.ServiceProxyBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Dimension
import java.io.IOException
import java.util.*

/**
 * Sets up the SourceMarker plugin by configuring and initializing the various plugin modules.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
object SourceMarkerPlugin {

    val SOURCE_RED = Color(225, 72, 59)
    val INSTANCE_ID = UUID.randomUUID().toString()

    private val log = LoggerFactory.getLogger(SourceMarkerPlugin::class.java)
    private val deploymentIds = mutableListOf<String>()
    val vertx: Vertx
    private var connectionJob: Job? = null

    /**
     * Setup Vert.x EventBus for communication between plugin modules.
     */
    init {
        SourceMarker.enabled = false
        val options = if (System.getProperty("sourcemarker.debug.unblocked_threads", "false")!!.toBoolean()) {
            log.info("Removed blocked thread checker")
            VertxOptions().setBlockedThreadCheckInterval(Int.MAX_VALUE.toLong())
        } else {
            VertxOptions()
        }
        vertx = Vertx.vertx(options)
        log.debug("Registering SourceMarker protocol codecs")
        vertx.eventBus().registerDefaultCodec(SourcePortal::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(ArtifactMetricResult::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(TraceResult::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(TraceStack::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(TraceSpanStackQueryResult::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(EndpointResult::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(JvmStackTraceElement::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(ArtifactQualifiedName::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(LogResult::class.java, LocalMessageCodec())
        vertx.eventBus().registerDefaultCodec(TraceSpan::class.java, LocalMessageCodec())

        val module = SimpleModule()
        module.addSerializer(Instant::class.java, KSerializers.KotlinInstantSerializer())
        module.addDeserializer(Instant::class.java, KSerializers.KotlinInstantDeserializer())
        DatabindCodec.mapper().registerModule(module)

        DatabindCodec.mapper().registerModule(GuavaModule())
        DatabindCodec.mapper().registerModule(Jdk8Module())
        DatabindCodec.mapper().registerModule(JavaTimeModule())
        DatabindCodec.mapper().registerModule(KotlinModule())
        DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    }

    suspend fun init(project: Project) {
        log.info("Initializing SourceMarkerPlugin on project: {}", project)
        restartIfNecessary()

        val projectSettings = PropertiesComponent.getInstance(project)
        val config = if (projectSettings.isValueSet("sourcemarker_plugin_config")) {
            try {
                Json.decodeValue(
                    projectSettings.getValue("sourcemarker_plugin_config"),
                    SourceMarkerConfig::class.java
                )
            } catch (ex: DecodeException) {
                log.warn("Failed to decode SourceMarker configuration", ex)
                projectSettings.unsetValue("sourcemarker_plugin_config")
                SourceMarkerConfig()
            }
        } else {
            SourceMarkerConfig()
        }

        //attempt to determine root source package automatically (if necessary)
        val checkRootPackage = Promise.promise<Nothing>()
        if (config.rootSourcePackage.isNullOrBlank()) {
            ApplicationManager.getApplication().runReadAction {
                var basePackages = JavaPsiFacade.getInstance(project).findPackage("")
                    ?.getSubPackages(ProjectScope.getProjectScope(project))

                //remove non-code packages
                basePackages = basePackages!!.filter {
                    val dirs = it.directories
                    dirs.isNotEmpty() && !dirs[0].virtualFile.path.contains("/src/main/resources/")
                }.toTypedArray()
                basePackages = basePackages.filter {
                    it.qualifiedName != "asciidoc" && it.qualifiedName != "lib"
                }.toTypedArray() //todo: probably shouldn't be necessary

                //determine deepest common source package
                if (basePackages.isNotEmpty()) {
                    var rootPackage: String? = null
                    while (basePackages!!.size == 1) {
                        rootPackage = basePackages[0]!!.qualifiedName
                        basePackages = basePackages[0]!!.getSubPackages(ProjectScope.getProjectScope(project))
                    }
                    if (rootPackage != null) {
                        log.info("Detected root source package: $rootPackage")
                        config.rootSourcePackage = rootPackage
                        projectSettings.setValue("sourcemarker_plugin_config", Json.encode(config))
                    }
                }
                checkRootPackage.complete()
            }
        } else {
            checkRootPackage.complete()
        }

        checkRootPackage.future().onComplete {
            connectionJob?.cancel()
            connectionJob = null

            connectionJob = GlobalScope.launch(vertx.dispatcher()) {
                var connectedMonitor = false
                try {
                    initServices(project, config)
                    initMonitor(config)
                    connectedMonitor = true
                } catch (ignored: CancellationException) {
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
                    log.error("Connection failed. Reason: {}", throwable.message)
                }

                discoverAvailableServices(config, project)
                if (connectedMonitor) {
                    initPortal(config)
                    initMarker(config, project)
                    initMapper()
                }
            }
        }
    }

    private suspend fun discoverAvailableServices(config: SourceMarkerConfig, project: Project) {
        val hardcodedConfig: JsonObject = try {
            JsonObject(
                Resources.toString(
                    Resources.getResource(javaClass, "/plugin-configuration.json"), Charsets.UTF_8
                )
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        if (!hardcodedConfig.getJsonObject("visible_settings").getBoolean("service_discovery")) {
            return
        }

        val discovery: ServiceDiscovery = DiscoveryImpl(
            vertx,
            ServiceDiscoveryOptions().setBackendConfiguration(
                JsonObject()
                    .put("backend-name", "tcp-service-discovery")
                    .put("hardcoded_config", hardcodedConfig)
                    .put("sourcemarker_plugin_config", JsonObject.mapFrom(config))
            )
        )
        val availableRecords = discovery.getRecords { true }.await()

        //local tracing
        if (hardcodedConfig.getJsonObject("services").getBoolean("local_tracing")) {
            if (availableRecords.any { it.name == SourceMarkerServices.Utilize.LOCAL_TRACING }) {
                log.info("Local tracing available")
                Instance.localTracing = ServiceProxyBuilder(vertx)
                    .setToken(config.serviceToken!!)
                    .setAddress(SourceMarkerServices.Utilize.LOCAL_TRACING)
                    .build(LocalTracingService::class.java)
            } else {
                log.warn("Local tracing unavailable")
            }
        } else {
            log.info("Local tracing disabled")
        }

        //log count indicator
        if (hardcodedConfig.getJsonObject("services").getBoolean("log_count_indicator")) {
            if (availableRecords.any { it.name == SourceMarkerServices.Utilize.LOG_COUNT_INDICATOR }) {
                log.info("Log count indicator available")
                Instance.logCountIndicator = ServiceProxyBuilder(vertx)
                    .setToken(config.serviceToken!!)
                    .setAddress(SourceMarkerServices.Utilize.LOG_COUNT_INDICATOR)
                    .build(LogCountIndicatorService::class.java)

                GlobalScope.launch(vertx.dispatcher()) {
                    deploymentIds.add(vertx.deployVerticle(LogCountIndicators()).await())
                }
            } else {
                log.warn("Log count indicator unavailable")
            }
        } else {
            log.info("Log count indicator disabled")
        }

        //live instrument
        if (hardcodedConfig.getJsonObject("services").getBoolean("live_instrument")) {
            if (availableRecords.any { it.name == SourceMarkerServices.Utilize.LIVE_INSTRUMENT }) {
                log.info("Live instruments available")
                SourceMarker.addGlobalSourceMarkEventListener(LiveLogStatusManager)

                Instance.liveInstrument = ServiceProxyBuilder(vertx)
                    .setToken(config.serviceToken!!)
                    .setAddress(SourceMarkerServices.Utilize.LIVE_INSTRUMENT)
                    .build(LiveInstrumentService::class.java)
                ApplicationManager.getApplication().invokeLater {
                    BreakpointHitWindowService.getInstance(project).showEventsWindow()
                }
                val breakpointListener = LiveInstrumentManager(project)
                GlobalScope.launch(vertx.dispatcher()) {
                    deploymentIds.add(vertx.deployVerticle(breakpointListener).await())
                }
                project.messageBus.connect().subscribe(XBreakpointListener.TOPIC, breakpointListener)
            } else {
                log.warn("Live instruments unavailable")
            }
        } else {
            log.info("Live instruments disabled")
        }

        //live view
        if (hardcodedConfig.getJsonObject("services").getBoolean("live_view")) {
            if (availableRecords.any { it.name == SourceMarkerServices.Utilize.LIVE_VIEW }) {
                log.info("Live views available")
                Instance.liveView = ServiceProxyBuilder(vertx)
                    .setToken(config.serviceToken!!)
                    .setAddress(SourceMarkerServices.Utilize.LIVE_VIEW)
                    .build(LiveViewService::class.java)

                val viewListener = LiveViewManager(project)
                GlobalScope.launch(vertx.dispatcher()) {
                    deploymentIds.add(vertx.deployVerticle(viewListener).await())
                }
            } else {
                log.warn("Live views unavailable")
            }
        } else {
            log.info("Live views disabled")
        }
    }

    private suspend fun restartIfNecessary() {
        val clearMarkers = Promise.promise<Nothing>()
        ApplicationManager.getApplication().runReadAction {
            if (SourceMarker.enabled) {
                SourceMarker.clearAvailableSourceFileMarkers()
                SourceMarker.clearGlobalSourceMarkEventListeners()
            }
            clearMarkers.complete()
        }

        deploymentIds.forEach { vertx.undeploy(it).await() }
        deploymentIds.clear()
        clearMarkers.future().await()

        TCPServiceDiscoveryBackend.socket?.close()?.await()
        TCPServiceDiscoveryBackend.socket = null
    }

    private suspend fun initServices(project: Project, config: SourceMarkerConfig) {
        val hardcodedConfig: JsonObject = try {
            JsonObject(
                Resources.toString(
                    Resources.getResource(javaClass, "/plugin-configuration.json"), Charsets.UTF_8
                )
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val serviceDiscoveryEnabled = hardcodedConfig.getJsonObject("visible_settings")
            .getBoolean("service_discovery")
        if (serviceDiscoveryEnabled && !config.serviceHost.isNullOrBlank()) {
            val servicePort = config.getServicePortNormalized(hardcodedConfig.getInteger("service_port"))!!
            val certificatePins = mutableListOf<String>()
            certificatePins.addAll(config.certificatePins)
            val hardcodedPin = hardcodedConfig.getString("certificate_pin")
            if (!hardcodedPin.isNullOrBlank()) {
                certificatePins.add(hardcodedPin)
            }
            val httpClientOptions = if (certificatePins.isNotEmpty()) {
                HttpClientOptions()
                    .setTrustOptions(
                        TrustOptions.wrap(
                            JavaPinning.trustManagerForPins(certificatePins.map { Pin.fromString("CERTSHA256:$it") })
                        )
                    )
                    .setVerifyHost(false)
            } else {
                HttpClientOptions()
            }

            val tokenUri = hardcodedConfig.getString("token_uri") + "?access_token=" + config.accessToken
            val req = vertx.createHttpClient(httpClientOptions).request(
                RequestOptions()
                    .setSsl(config.isSsl())
                    .setHost(config.serviceHostNormalized!!)
                    .setPort(servicePort)
                    .setURI(tokenUri)
            ).await()
            req.end().await()
            val resp = req.response().await()
            if (resp.statusCode() in 200..299) {
                val body = resp.body().await().toString()
                config.serviceToken = body
            } else {
                config.serviceToken = null

                log.error("Invalid access token")
                Notifications.Bus.notify(
                    Notification(
                        message("plugin_name"), "Invalid access token",
                        "Failed to validate access token",
                        NotificationType.ERROR
                    )
                )
            }
        } else if (serviceDiscoveryEnabled) {
            //try default local access
            val req = vertx.createHttpClient().request(
                RequestOptions()
                    .setHost("localhost")
                    .setPort(hardcodedConfig.getInteger("service_port"))
                    .setURI(hardcodedConfig.getString("token_uri"))
            ).await()
            req.end().await()
            val resp = req.response().await()
            if (resp.statusCode() in 200..299) {
                val body = resp.body().await().toString()
                config.serviceToken = body
                config.serviceHost = "localhost"

                val projectSettings = PropertiesComponent.getInstance(project)
                projectSettings.setValue("sourcemarker_plugin_config", Json.encode(config))

                //auto-established notification
                Notifications.Bus.notify(
                    Notification(
                        message("plugin_name"), "Connection auto-established",
                        "You have successfully auto-connected. ${message("plugin_name")} is now fully activated.",
                        NotificationType.INFORMATION
                    )
                )
            }
        }
    }

    private suspend fun initMonitor(config: SourceMarkerConfig) {
        val hardcodedConfig: JsonObject = try {
            JsonObject(
                Resources.toString(
                    Resources.getResource(javaClass, "/plugin-configuration.json"), Charsets.UTF_8
                )
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val serviceDiscoveryEnabled = hardcodedConfig.getJsonObject("visible_settings")
            .getBoolean("service_discovery")
        var skywalkingHost = config.skywalkingOapUrl
        if (serviceDiscoveryEnabled && !config.serviceHost.isNullOrBlank()) {
            val scheme = if (config.isSsl()) "https" else "http"
            skywalkingHost = "$scheme://${config.serviceHostNormalized}:" +
                    "${config.getServicePortNormalized(hardcodedConfig.getInteger("service_port"))}" +
                    "/graphql/skywalking"
        }

        val certificatePins = mutableListOf<String>()
        certificatePins.addAll(config.certificatePins)
        val hardcodedPin = hardcodedConfig.getString("certificate_pin")
        if (!hardcodedPin.isNullOrBlank()) {
            certificatePins.add(hardcodedPin)
        }

        deploymentIds.add(
            vertx.deployVerticle(
                SkywalkingMonitor(skywalkingHost, config.serviceToken, certificatePins)
            ).await()
        )
    }

    private fun initMapper() {
        //todo: this
    }

    private suspend fun initPortal(config: SourceMarkerConfig) {
        val hardcodedConfig: JsonObject = try {
            JsonObject(
                Resources.toString(
                    Resources.getResource(javaClass, "/plugin-configuration.json"), Charsets.UTF_8
                )
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val serviceDiscoveryEnabled = hardcodedConfig.getJsonObject("visible_settings")
            .getBoolean("service_discovery")

        //todo: portal should be connected to event bus without bridge
        val sockJSHandler = SockJSHandler.create(vertx)
        val portalBridgeOptions = SockJSBridgeOptions()
            .addInboundPermitted(PermittedOptions().setAddressRegex(".+"))
            .addOutboundPermitted(PermittedOptions().setAddressRegex(".+"))
        sockJSHandler.bridge(portalBridgeOptions)

        val router = Router.router(vertx)
        router.route("/eventbus/*").handler(sockJSHandler)
        val bridgePort = vertx.sharedData().getLocalMap<String, Int>("portal")
            .getOrDefault("bridge.port", 0)
        if (bridgePort != 0) {
            log.info("Starting bridge server on port: {}", bridgePort)
        }
        val bridgeServer = vertx.createHttpServer().requestHandler(router).listen(bridgePort, "localhost").await()

        //todo: load portal config (custom themes, etc)
        deploymentIds.add(
            vertx.deployVerticle(
                PortalServer(
                    bridgeServer.actualPort(),
                    config.portalRefreshIntervalMs,
                    !serviceDiscoveryEnabled
                )
            ).await()
        )
        deploymentIds.add(vertx.deployVerticle(PortalEventListener(config)).await())
    }

    private fun initMarker(config: SourceMarkerConfig, project: Project) {
        SourceMarker.addGlobalSourceMarkEventListener(PluginSourceMarkEventListener())

        val gutterMarkConfig = GutterMarkConfiguration()
        gutterMarkConfig.activateOnMouseHover = false
        gutterMarkConfig.activateOnKeyboardShortcut = true
        val componentProvider = SourceMarkSingleJcefComponentProvider().apply {
            defaultConfiguration.preloadJcefBrowser = false
            defaultConfiguration.componentSizeEvaluator = object : ComponentSizeEvaluator() {
                override fun getDynamicSize(
                    editor: Editor,
                    configuration: SourceMarkComponentConfiguration
                ): Dimension {
                    var portalWidth = (editor.contentComponent.width * 0.8).toInt()
                    if (portalWidth > 775) {
                        portalWidth = 775
                    }
                    return Dimension(portalWidth, 250)
                }
            }
        }
        gutterMarkConfig.componentProvider = componentProvider

        SourceMarker.configuration.gutterMarkConfiguration = gutterMarkConfig
        SourceMarker.configuration.inlayMarkConfiguration.componentProvider = componentProvider
        SourceMarker.configuration.inlayMarkConfiguration.strictlyManualCreation = true

        if (config.rootSourcePackage != null) {
            SourceMarker.configuration.createSourceMarkFilter = CreateSourceMarkFilter { artifactQualifiedName ->
                artifactQualifiedName.startsWith(config.rootSourcePackage!!)
            }
        } else {
            log.warn("Could not determine root source package. Skipped adding create source mark filter...")
        }
        SourceMarker.enabled = true

        //force marker re-processing
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    /**
     * Used to transmit protocol messages.
     *
     * @since 0.1.0
     */
    class LocalMessageCodec<T> : MessageCodec<T, T> {
        override fun encodeToWire(buffer: Buffer, o: T): Unit =
            throw UnsupportedOperationException("Not supported yet.")

        override fun decodeFromWire(pos: Int, buffer: Buffer): T =
            throw UnsupportedOperationException("Not supported yet.")

        override fun transform(o: T): T = o
        override fun name(): String = UUID.randomUUID().toString()
        override fun systemCodecID(): Byte = -1
    }

    /**
     * Used to serialize/deserialize Kotlin classes.
     *
     * @since 0.1.0
     */
    class KSerializers {
        /**
         * Used to serialize [Instant] classes.
         *
         * @since 0.1.0
         */
        class KotlinInstantSerializer : JsonSerializer<Instant>() {
            override fun serialize(value: Instant, jgen: JsonGenerator, provider: SerializerProvider) =
                jgen.writeNumber(value.toEpochMilliseconds())
        }

        /**
         * Used to deserialize [Instant] classes.
         *
         * @since 0.1.0
         */
        class KotlinInstantDeserializer : JsonDeserializer<Instant>() {
            override fun deserialize(p: JsonParser, p1: DeserializationContext): Instant =
                Instant.fromEpochMilliseconds((p.codec.readTree(p) as JsonNode).longValue())
        }
    }
}
