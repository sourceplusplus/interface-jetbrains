package spp.jetbrains.sourcemarker

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
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
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import eu.geekplace.javapinning.JavaPinning
import eu.geekplace.javapinning.pin.Pin
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
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
import org.apache.commons.text.CaseUtils
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.*
import spp.jetbrains.marker.py.PythonArtifactCreationService
import spp.jetbrains.marker.py.PythonArtifactNamingService
import spp.jetbrains.marker.py.PythonArtifactScopeService
import spp.jetbrains.marker.py.PythonConditionParser
import spp.jetbrains.marker.source.mark.api.component.api.config.ComponentSizeEvaluator
import spp.jetbrains.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import spp.jetbrains.marker.source.mark.api.component.jcef.SourceMarkSingleJcefComponentProvider
import spp.jetbrains.marker.source.mark.api.filter.CreateSourceMarkFilter
import spp.jetbrains.marker.source.mark.gutter.config.GutterMarkConfiguration
import spp.jetbrains.monitor.skywalking.SkywalkingMonitor
import spp.jetbrains.portal.SourcePortal
import spp.jetbrains.portal.backend.PortalServer
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity.Companion.INTELLIJ_PRODUCT_CODES
import spp.jetbrains.sourcemarker.activities.PluginSourceMarkerStartupActivity.Companion.PYCHARM_PRODUCT_CODES
import spp.jetbrains.sourcemarker.discover.TCPServiceDiscoveryBackend
import spp.jetbrains.sourcemarker.listeners.PluginSourceMarkEventListener
import spp.jetbrains.sourcemarker.listeners.PortalEventListener
import spp.jetbrains.sourcemarker.service.LiveInstrumentManager
import spp.jetbrains.sourcemarker.service.LiveViewManager
import spp.jetbrains.sourcemarker.service.LogCountIndicators
import spp.jetbrains.sourcemarker.service.breakpoint.BreakpointHitWindowService
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.jetbrains.sourcemarker.settings.getServicePortNormalized
import spp.jetbrains.sourcemarker.settings.isSsl
import spp.jetbrains.sourcemarker.settings.serviceHostNormalized
import spp.jetbrains.sourcemarker.status.LiveStatusManager
import spp.protocol.SourceMarkerServices
import spp.protocol.SourceMarkerServices.Instance
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.endpoint.EndpointResult
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.metrics.ArtifactMetricResult
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpan
import spp.protocol.artifact.trace.TraceSpanStackQueryResult
import spp.protocol.artifact.trace.TraceStack
import spp.protocol.service.LiveService
import spp.protocol.service.live.LiveInstrumentService
import spp.protocol.service.live.LiveViewService
import spp.protocol.service.logging.LogCountIndicatorService
import spp.protocol.service.tracing.LocalTracingService
import spp.protocol.util.KSerializers
import spp.protocol.util.LocalMessageCodec
import java.awt.Color
import java.awt.Dimension
import java.io.File
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
        vertx.eventBus().registerDefaultCodec(LiveStackTraceElement::class.java, LocalMessageCodec())
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

        val productCode = ApplicationInfo.getInstance().build.productCode
        if (PYCHARM_PRODUCT_CODES.contains(productCode)) {
            SourceMarker.creationService = PythonArtifactCreationService()
            SourceMarker.namingService = PythonArtifactNamingService()
            SourceMarker.scopeService = PythonArtifactScopeService()
            SourceMarker.conditionParser = PythonConditionParser()
        } else if (INTELLIJ_PRODUCT_CODES.contains(productCode)) {
            SourceMarker.creationService = JVMArtifactCreationService()
            SourceMarker.namingService = JVMArtifactNamingService()
            SourceMarker.scopeService = JVMArtifactScopeService()
            SourceMarker.conditionParser = JVMConditionParser()
        } else {
            val pluginName = message("plugin_name")
            Notifications.Bus.notify(
                Notification(
                    pluginName,
                    "Unsupported product code",
                    "Unsupported product code: $productCode.",
                    NotificationType.ERROR
                )
            )
            throw IllegalStateException("Unsupported product code: $productCode")
        }
    }

    private fun loadDefaultConfiguration(project: Project): SourceMarkerConfig {
        if (project.basePath != null) {
            val configFile = File(project.basePath, ".spp/spp-plugin.yml")
            if (configFile.exists()) {
                val config = JsonObject(
                    ObjectMapper().writeValueAsString(YAMLMapper().readValue(configFile, Object::class.java))
                )
                config.fieldNames().toList().forEach {
                    config.put(CaseUtils.toCamelCase(it, false, '_'), config.getValue(it))
                    config.remove(it)
                }
                PropertiesComponent.getInstance(project).setValue("sourcemarker_plugin_config", config.toString())
                return Json.decodeValue(config.toString(), SourceMarkerConfig::class.java)
            }
        }

        return SourceMarkerConfig()
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
            loadDefaultConfiguration(project)
        }

        //attempt to determine root source package automatically (if necessary)
        if (config.rootSourcePackages.isEmpty()) {
            if (INTELLIJ_PRODUCT_CODES.contains(ApplicationInfo.getInstance().build.productCode)) {
                val rootPackage = ArtifactSearch.detectRootPackage(project)
                if (rootPackage != null) {
                    log.info("Detected root source package: $rootPackage")
                    config.rootSourcePackages = listOf(rootPackage)
                    projectSettings.setValue("sourcemarker_plugin_config", Json.encode(config))
                }
            }
        }

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

        val discovery: ServiceDiscovery = DiscoveryImpl(
            vertx,
            ServiceDiscoveryOptions().setBackendConfiguration(
                JsonObject()
                    .put("backend-name", "tcp-service-discovery")
                    .put("hardcoded_config", hardcodedConfig)
                    .put("sourcemarker_plugin_config", JsonObject.mapFrom(config))
            )
        )

        log.info("Discovering available services")
        val availableRecords = discovery.getRecords { true }.await()

        //live service
        if (availableRecords.any { it.name == SourceMarkerServices.Utilize.LIVE_SERVICE }) {
            log.info("Live service available")

            Instance.liveService = ServiceProxyBuilder(vertx)
                .apply { config.serviceToken?.let { setToken(it) } }
                .setAddress(SourceMarkerServices.Utilize.LIVE_SERVICE)
                .build(LiveService::class.java)
        } else {
            log.warn("Live service unavailable")
        }

        //live instrument
        if (hardcodedConfig.getJsonObject("services").getBoolean("live_instrument")) {
            if (availableRecords.any { it.name == SourceMarkerServices.Utilize.LIVE_INSTRUMENT }) {
                log.info("Live instruments available")
                SourceMarker.addGlobalSourceMarkEventListener(LiveStatusManager)

                Instance.liveInstrument = ServiceProxyBuilder(vertx)
                    .apply { config.serviceToken?.let { setToken(it) } }
                    .setAddress(SourceMarkerServices.Utilize.LIVE_INSTRUMENT)
                    .build(LiveInstrumentService::class.java)
                ApplicationManager.getApplication().invokeLater {
                    BreakpointHitWindowService.getInstance(project).showEventsWindow()
                }
                val breakpointListener = LiveInstrumentManager(project)
                GlobalScope.launch(vertx.dispatcher()) {
                    deploymentIds.add(vertx.deployVerticle(breakpointListener).await())
                }
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
                    .apply { config.serviceToken?.let { setToken(it) } }
                    .setAddress(SourceMarkerServices.Utilize.LIVE_VIEW)
                    .build(LiveViewService::class.java)

                val viewListener = LiveViewManager(config)
                GlobalScope.launch(vertx.dispatcher()) {
                    deploymentIds.add(vertx.deployVerticle(viewListener).await())
                }
            } else {
                log.warn("Live views unavailable")
            }
        } else {
            log.info("Live views disabled")
        }

        //local tracing
        if (hardcodedConfig.getJsonObject("services").getBoolean("local_tracing")) {
            if (availableRecords.any { it.name == SourceMarkerServices.Utilize.LOCAL_TRACING }) {
                log.info("Local tracing available")
                Instance.localTracing = ServiceProxyBuilder(vertx)
                    .apply { config.serviceToken?.let { setToken(it) } }
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
                    .apply { config.serviceToken?.let { setToken(it) } }
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

        if (!config.serviceHost.isNullOrBlank()) {
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
                HttpClientOptions().apply {
                    if (config.isSsl()) {
                        isSsl = config.isSsl()
                        isVerifyHost = false
                        isTrustAll = true
                    }
                }
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
                if (resp.statusCode() != 202) {
                    config.serviceToken = body
                }
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
        } else {
            //try default local access
            val defaultAccessToken = "change-me"
            val tokenUri = hardcodedConfig.getString("token_uri") + "?access_token=$defaultAccessToken"
            val req = vertx.createHttpClient(HttpClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true))
                .request(
                    RequestOptions()
                        .setHost("localhost")
                        .setPort(hardcodedConfig.getInteger("service_port"))
                        .setURI(tokenUri)
                ).await()
            req.end().await()
            val resp = req.response().await()
            if (resp.statusCode() in 200..299) {
                val body = resp.body().await().toString()
                config.serviceToken = body
                config.serviceHost = "https://localhost:" + hardcodedConfig.getInteger("service_port")
                config.accessToken = defaultAccessToken
                config.verifyHost = false

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

        val scheme = if (config.isSsl()) "https" else "http"
        val skywalkingHost = "$scheme://${config.serviceHostNormalized}:" +
                "${config.getServicePortNormalized(hardcodedConfig.getInteger("service_port"))}" +
                "/graphql/skywalking"

        val certificatePins = mutableListOf<String>()
        certificatePins.addAll(config.certificatePins)
        val hardcodedPin = hardcodedConfig.getString("certificate_pin")
        if (!hardcodedPin.isNullOrBlank()) {
            certificatePins.add(hardcodedPin)
        }

        deploymentIds.add(
            vertx.deployVerticle(
                SkywalkingMonitor(skywalkingHost, config.serviceToken, certificatePins, config.verifyHost)
            ).await()
        )
    }

    private fun initMapper() {
        //todo: this
    }

    private suspend fun initPortal(config: SourceMarkerConfig) {
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
        deploymentIds.add(vertx.deployVerticle(PortalServer(bridgeServer.actualPort())).await())
        deploymentIds.add(vertx.deployVerticle(PortalEventListener(config)).await())
    }

    private fun initMarker(config: SourceMarkerConfig, project: Project) {
        log.info("Initializing marker")
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

        if (config.rootSourcePackages.isNotEmpty()) {
            SourceMarker.configuration.createSourceMarkFilter = CreateSourceMarkFilter { artifactQualifiedName ->
                config.rootSourcePackages.any { artifactQualifiedName.identifier.startsWith(it) }
            }
        } else {
            val productCode = ApplicationInfo.getInstance().build.productCode
            if (INTELLIJ_PRODUCT_CODES.contains(productCode)) {
                log.warn("Could not determine root source package. Skipped adding create source mark filter...")
            }
        }
        SourceMarker.enabled = true
        log.info("Source marker enabled")

        //force marker re-processing
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
