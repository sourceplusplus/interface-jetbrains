package com.sourceplusplus.sourcemarker.activities

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.ProjectScope
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.plugin.SourceMarkerStartupActivity
import com.sourceplusplus.marker.source.mark.api.component.api.config.ComponentSizeEvaluator
import com.sourceplusplus.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkSingleJcefComponentProvider
import com.sourceplusplus.marker.source.mark.api.component.jcef.config.BrowserLoadingListener
import com.sourceplusplus.marker.source.mark.api.component.jcef.config.SourceMarkJcefComponentConfiguration
import com.sourceplusplus.marker.source.mark.api.filter.CreateSourceMarkFilter
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.mentor.base.MentorJobConfig
import com.sourceplusplus.mentor.impl.job.ActiveExceptionMentor
import com.sourceplusplus.mentor.impl.job.RampDetectionMentor
import com.sourceplusplus.monitor.skywalking.SkywalkingMonitor
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.backend.PortalServer
import com.sourceplusplus.protocol.artifact.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult
import com.sourceplusplus.sourcemarker.listeners.ArtifactAdviceListener
import com.sourceplusplus.sourcemarker.listeners.PluginSourceMarkEventListener
import com.sourceplusplus.sourcemarker.listeners.PortalEventListener
import com.sourceplusplus.sourcemarker.psi.PluginSqlProducerSearch
import com.sourceplusplus.sourcemarker.settings.SourceMarkerConfig
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.Json
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.log4j.FileAppender
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PluginSourceMarkerStartupActivity : SourceMarkerStartupActivity(), Disposable {

    companion object {
        private val log = LoggerFactory.getLogger(PluginSourceMarkerStartupActivity::class.java)
        val vertx: Vertx = Vertx.vertx()

        fun registerCodecs(vertx: Vertx) {
            log.debug("Registering SourceMarker Protocol codecs")
            vertx.eventBus().registerDefaultCodec(SourcePortal::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(ArtifactMetricResult::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(TraceResult::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(TraceSpanStackQueryResult::class.java, LocalMessageCodec())

            DatabindCodec.mapper().registerModule(GuavaModule())
            DatabindCodec.mapper().registerModule(Jdk8Module())
            DatabindCodec.mapper().registerModule(JavaTimeModule())
            DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
        }
    }

    init {
        registerCodecs(vertx)
    }

    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return //todo: change when integration tests are added
        }

        val config = if (PropertiesComponent.getInstance().isValueSet("sourcemarker_plugin_config")) {
            Json.decodeValue(
                PropertiesComponent.getInstance().getValue("sourcemarker_plugin_config"),
                SourceMarkerConfig::class.java
            )
        } else {
            SourceMarkerConfig()
        }

        //attempt to determine root source package automatically (if necessary)
        if (config.rootSourcePackage == null) {
            var basePackages = JavaPsiFacade.getInstance(project).findPackage("")
                ?.getSubPackages(ProjectScope.getProjectScope(project))

            //remove non-code packages
            basePackages = basePackages!!.filter { it.qualifiedName != "asciidoc" }.toTypedArray()

            //determine deepest common source package
            if (basePackages.isNotEmpty()) {
                var rootPackage: String? = null
                while (basePackages!!.size == 1) {
                    rootPackage = basePackages[0]!!.qualifiedName
                    basePackages = basePackages[0]!!.getSubPackages(ProjectScope.getProjectScope(project))
                }
                if (rootPackage != null) {
                    config.rootSourcePackage = rootPackage
                    PropertiesComponent.getInstance().setValue("sourcemarker_plugin_config", Json.encode(config))
                }
            }
        }

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
            }

            if (connectedMonitor) {
                initPortal()
                initMarker(config)
                initMapper()
                initMentor(config)
            }
        }

        //debug logs
        val fa = FileAppender()
        fa.file = "/tmp/sourcemarker.log"
        fa.layout = PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n")
        fa.activateOptions()
        Logger.getLogger("com.sourceplusplus").addAppender(fa)

        super.runActivity(project)
    }

    private suspend fun initMonitor(config: SourceMarkerConfig) {
        vertx.deployVerticleAwait(SkywalkingMonitor(config.skywalkingOapUrl))
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
        SourceMarkerPlugin.addGlobalSourceMarkEventListener(mentorAdviceListener)
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

        vertx.deployVerticleAwait(mentor)
        return mentor
    }

    private suspend fun initPortal() {
        //todo: load portal config (custom themes, etc)
        vertx.deployVerticleAwait(PortalServer())
        vertx.deployVerticleAwait(PortalEventListener())

        //todo: portal should be connected to event bus without bridge
        val sockJSHandler = SockJSHandler.create(vertx)
        val portalBridgeOptions = SockJSBridgeOptions()
            .addInboundPermitted(PermittedOptions().setAddressRegex(".+"))
            .addOutboundPermitted(PermittedOptions().setAddressRegex(".+"))
        sockJSHandler.bridge(portalBridgeOptions)

        val router = Router.router(vertx)
        router.route("/eventbus/*").handler(sockJSHandler)
        vertx.createHttpServer().requestHandler(router).listenAwait(8888, "localhost")
    }

    private fun initMarker(config: SourceMarkerConfig) {
        SourceMarkerPlugin.addGlobalSourceMarkEventListener(PluginSourceMarkEventListener())

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
            defaultConfiguration.browserLoadingListener = object : BrowserLoadingListener() {
                override fun beforeBrowserCreated(configuration: SourceMarkJcefComponentConfiguration) {
                    configuration.initialUrl =
                        "http://localhost:8080/?portalUuid=${SourcePortal.getPortals()[0].portalUuid}"
                }
            }
        }
        gutterMarkConfig.componentProvider = componentProvider

        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration = gutterMarkConfig
        SourceMarkerPlugin.configuration.defaultInlayMarkConfiguration.componentProvider = componentProvider

        if (config.rootSourcePackage != null) {
            SourceMarkerPlugin.configuration.createSourceMarkFilter = CreateSourceMarkFilter { artifactQualifiedName ->
                artifactQualifiedName.startsWith(config.rootSourcePackage!!)
            }
        } else {
            log.warn("Could not determine root source package. Skipped adding create source mark filter...")
        }
    }

    override fun dispose() {
        if (SourceMarkerPlugin.enabled) {
            SourceMarkerPlugin.clearAvailableSourceFileMarkers()
        }
        vertx.close()
    }

    /**
     * todo: description.
     *
     * @since 0.0.1
     */
    class LocalMessageCodec<T> internal constructor() : MessageCodec<T, T> {
        override fun encodeToWire(buffer: Buffer, o: T): Unit =
            throw UnsupportedOperationException("Not supported yet.")

        override fun decodeFromWire(pos: Int, buffer: Buffer): T =
            throw UnsupportedOperationException("Not supported yet.")

        override fun transform(o: T): T = o
        override fun name(): String = UUID.randomUUID().toString()
        override fun systemCodecID(): Byte = -1
    }
}
