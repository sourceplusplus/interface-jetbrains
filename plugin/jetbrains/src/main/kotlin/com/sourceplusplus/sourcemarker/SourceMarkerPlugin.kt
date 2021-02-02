package com.sourceplusplus.sourcemarker

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.ProjectScope
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.mark.api.component.api.config.ComponentSizeEvaluator
import com.sourceplusplus.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkSingleJcefComponentProvider
import com.sourceplusplus.marker.source.mark.api.filter.CreateSourceMarkFilter
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.mentor.base.MentorJobConfig
import com.sourceplusplus.mentor.impl.job.ActiveExceptionMentor
import com.sourceplusplus.mentor.impl.job.RampDetectionMentor
import com.sourceplusplus.monitor.skywalking.SkywalkingMonitor
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.backend.PortalServer
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.endpoint.EndpointResult
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.protocol.artifact.log.LogResult
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult
import com.sourceplusplus.protocol.artifact.trace.TraceStack
import com.sourceplusplus.sourcemarker.listeners.ArtifactAdviceListener
import com.sourceplusplus.sourcemarker.listeners.PluginSourceMarkEventListener
import com.sourceplusplus.sourcemarker.listeners.PortalEventListener
import com.sourceplusplus.sourcemarker.psi.PluginSqlProducerSearch
import com.sourceplusplus.sourcemarker.settings.SourceMarkerConfig
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.Json
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.util.*

/**
 * Sets up the SourceMarker plugin by configuring and initializing the various plugin modules.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
object SourceMarkerPlugin {

    private val log = LoggerFactory.getLogger(SourceMarkerPlugin::class.java)
    private val deploymentIds = mutableListOf<String>()
    val vertx: Vertx = Vertx.vertx()

    /**
     * Setup Vert.x EventBus for communication between plugin modules.
     */
    init {
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
        vertx.eventBus().registerDefaultCodec(SourceMarkerConfig::class.java, LocalMessageCodec())
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
            Json.decodeValue(
                projectSettings.getValue("sourcemarker_plugin_config"),
                SourceMarkerConfig::class.java
            )
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
            GlobalScope.launch(vertx.dispatcher()) {
                var connectedMonitor = false
                try {
                    initMonitor(config)
                    connectedMonitor = true
                } catch (throwable: Throwable) {
                    //todo: if first time bring up config panel automatically instead of notification
                    Notifications.Bus.notify(
                        Notification(
                            "SourceMarker", "Connection Failed",
                            "SourceMarker failed to connect to Apache SkyWalking. " +
                                    "Please ensure Apache SkyWalking is running and the correct configuration " +
                                    "is set at: Settings -> Tools -> SourceMarker",
                            NotificationType.ERROR
                        )
                    )
                    log.error("Connection failed", throwable)
                }

                if (connectedMonitor) {
                    initPortal(config)
                    initMarker(config)
                    initMapper()
                    initMentor(config)
                }
            }
        }
    }

    private suspend fun restartIfNecessary() {
        val clearMarkers = Promise.promise<Nothing>()
        ApplicationManager.getApplication().runReadAction {
            SourceMarker.clearAvailableSourceFileMarkers()
            clearMarkers.complete()
        }

        deploymentIds.forEach { vertx.undeploy(it).await() }
        deploymentIds.clear()
        clearMarkers.future().await()
    }

    private suspend fun initMonitor(config: SourceMarkerConfig) {
        deploymentIds.add(vertx.deployVerticle(SkywalkingMonitor(config.skywalkingOapUrl)).await())
    }

    private fun initMapper() {
        //todo: this
    }

    /**
     * Schedules long running, generic, and low-priority mentor jobs.
     * High-priority, specific, and short running mentor jobs are executed during source code navigation.
     */
    private suspend fun initMentor(config: SourceMarkerConfig): SourceMentor {
        val mentor = SourceMentor()
        val mentorAdviceListener = ArtifactAdviceListener()
        SourceMarker.addGlobalSourceMarkEventListener(mentorAdviceListener)
        mentor.addAdviceListener(mentorAdviceListener)

        //configure and add long running low-priority mentor jobs
        mentor.addJobs(
            RampDetectionMentor(
                vertx, PluginSqlProducerSearch()
            ).withConfig(
                MentorJobConfig(
                    repeatForever = true,
                    repeatDelay = 30_000
                    //todo: configurable schedule
                )
            )
        )
        if (config.rootSourcePackage != null) {
            mentor.addJob(
                ActiveExceptionMentor(
                    vertx, config.rootSourcePackage!!
                ).withConfig(
                    MentorJobConfig(
                        repeatForever = true,
                        repeatDelay = 30_000
                        //todo: configurable schedule
                    )
                )
            )
        } else {
            log.warn("Could not determine root source package. Skipped adding ActiveExceptionMentor...")
        }

        deploymentIds.add(vertx.deployVerticle(mentor).await())
        return mentor
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

    private fun initMarker(config: SourceMarkerConfig) {
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
