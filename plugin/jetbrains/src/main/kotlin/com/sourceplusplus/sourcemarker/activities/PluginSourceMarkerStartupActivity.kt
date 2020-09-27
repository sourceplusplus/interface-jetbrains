package com.sourceplusplus.sourcemarker.activities

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.plugin.SourceMarkerStartupActivity
import com.sourceplusplus.marker.source.mark.api.component.api.config.ComponentSizeEvaluator
import com.sourceplusplus.marker.source.mark.api.component.api.config.SourceMarkComponentConfiguration
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkSingleJcefComponentProvider
import com.sourceplusplus.marker.source.mark.api.component.jcef.config.BrowserLoadingListener
import com.sourceplusplus.marker.source.mark.api.component.jcef.config.SourceMarkJcefComponentConfiguration
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.mentor.MentorJobConfig
import com.sourceplusplus.mentor.SourceMentor
import com.sourceplusplus.mentor.job.ActiveExceptionMentor
import com.sourceplusplus.mentor.job.RampDetectionMentor
import com.sourceplusplus.monitor.skywalking.SkywalkingMonitor
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.backend.PortalServer
import com.sourceplusplus.protocol.artifact.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpanStackQueryResult
import com.sourceplusplus.sourcemarker.listeners.PluginSourceMarkEventListener
import com.sourceplusplus.sourcemarker.listeners.PortalEventListener
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
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
            DatabindCodec.mapper().propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
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

        initPortal()
        initMarker(initMentor())
        initMapper()
        initMonitor()
        super.runActivity(project)
    }

    private fun initMonitor() {
        //todo: configurable
        val config = JsonObject().apply {
            put("graphql_endpoint", "http://localhost:12800/graphql")
        }
        vertx.deployVerticle(SkywalkingMonitor(), DeploymentOptions().setConfig(config))
    }

    private fun initMapper() {
        //todo: this
    }

    /**
     * Schedules long running, generic, and low-priority mentor jobs.
     * High-priority, specific, and short running mentor jobs are executed during source code navigation.
     */
    private fun initMentor(): SourceMentor {
        val mentor = SourceMentor()
        mentor.executeJobs(
            ActiveExceptionMentor(vertx).withConfig(
                MentorJobConfig(
                    repeatForever = true
                    //todo: configurable schedule
                )
            ),
            RampDetectionMentor(vertx).withConfig(
                MentorJobConfig(
                    repeatForever = true
                    //todo: configurable schedule
                )
            )
        )
        return mentor
    }

    private fun initPortal() {
        //todo: load portal config (custom themes, etc)
        vertx.deployVerticle(PortalServer())
        vertx.deployVerticle(PortalEventListener())

        //todo: portal should be connected to event bus without bridge
        val sockJSHandler = SockJSHandler.create(vertx)
        val portalBridgeOptions = SockJSBridgeOptions()
            .addInboundPermitted(PermittedOptions().setAddressRegex(".+"))
            .addOutboundPermitted(PermittedOptions().setAddressRegex(".+"))
        sockJSHandler.bridge(portalBridgeOptions)

        val router = Router.router(vertx)
        router.route("/eventbus/*").handler(sockJSHandler)
        vertx.createHttpServer().requestHandler(router).listen(8888, "localhost")
    }

    private fun initMarker(sourceMentor: SourceMentor) {
        SourceMarkerPlugin.addGlobalSourceMarkEventListener(PluginSourceMarkEventListener(sourceMentor))

        val configuration = GutterMarkConfiguration()
        configuration.activateOnMouseHover = false
        configuration.activateOnKeyboardShortcut = true
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
                        "http://localhost:8080/overview?portal_uuid=${SourcePortal.getPortals()[0].portalUuid}"
                }
            }
        }
        configuration.componentProvider = componentProvider

        SourceMarkerPlugin.configuration.defaultGutterMarkConfiguration = configuration
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
