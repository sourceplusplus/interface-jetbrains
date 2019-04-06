package com.sourceplusplus.tooltip

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.artifact.SourceArtifactVersion
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.config.SourceTooltipConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetrics
import com.sourceplusplus.api.model.metric.TimeFramedMetricType
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.tooltip.coordinate.track.TooltipViewTracker
import com.sourceplusplus.tooltip.display.TooltipUI
import com.sourceplusplus.tooltip.display.tabs.OverviewTab
import com.sourceplusplus.tooltip.display.tabs.TracesTab
import io.vertx.core.*
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.net.JksOptions
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.apache.commons.io.IOUtils
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.PatternLayout
import org.modellwerkstatt.javaxbus.EventBus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets

/**
 * Used to bootstrap the Source++ Tooltip.
 *
 * The tooltip is able to fetch and display runtime behavior without interacting with the Source++ Plugin.
 * This allows the tooltip to be independently outside the IDE.
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TooltipBootstrap extends AbstractVerticle {

    //todo: fix https://github.com/CodeBrig/Source/issues/1 and remove static block below
    static {
        ConsoleAppender console = new ConsoleAppender()
        console.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"))
        console.setThreshold(Level.DEBUG)
        console.activateOptions()

        org.apache.log4j.Logger.rootLogger.loggerRepository.resetConfiguration()
        org.apache.log4j.Logger.getLogger("com.sourceplusplus").addAppender(console)
    }

    public static final MetricRegistry tooltipMetrics = new MetricRegistry()
    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final SourceCoreClient coreClient
    private final boolean pluginAvailable

    static void main(String[] args) {
        def configJSON
        def configFile = System.getenv("SOURCE_CONFIG")
        if (!configFile) {
            throw new RuntimeException("Missing SOURCE_CONFIG system environment!")
        }

        log.info("Using configuration file: $configFile")
        def configInputStream
        if (new File(configFile).exists()) {
            configInputStream = new File(configFile).newInputStream()
        } else {
            configInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config/$configFile")
        }
        def configData = IOUtils.toString(configInputStream, StandardCharsets.UTF_8)
        configJSON = new JsonObject(configData)

        def vertxOptions = new VertxOptions()
//        if (BUILD.getString("version") == "dev") {
//            //allow debug pauses
//            vertxOptions.setBlockedThreadCheckInterval(Integer.MAX_VALUE)
//        }

        def appUuid = configJSON.getString("app_uuid")
        SourceTooltipConfig.current.appUuid = appUuid
        //def artifactQualifiedName = configJSON.getString("artifact_qualified_name")
        //TooltipViewTracker.viewingTooltipArtifact = artifactQualifiedName
        def apiConfig = configJSON.getJsonObject("api")

        def coreClient = new SourceCoreClient(
                apiConfig.getString("host"), apiConfig.getInteger("port"), apiConfig.getBoolean("ssl"))
        if (apiConfig.getString("key")) {
            coreClient.setApiKey(apiConfig.getString("key"))
        }

        Vertx.vertx(vertxOptions).deployVerticle(new TooltipBootstrap(coreClient, false),
                new DeploymentOptions().setConfig(configJSON))
    }

    TooltipBootstrap(SourceCoreClient coreClient, boolean pluginAvailable) {
        this.coreClient = Objects.requireNonNull(coreClient)
        this.pluginAvailable = pluginAvailable
    }

    @Override
    void start(Future<Void> startFuture) throws Exception {
        if (pluginAvailable) {
            TooltipUI.preloadTooltipUI(vertx)
            SourceTooltipConfig.current.appUuid = SourcePluginConfig.current.appUuid
        } else {
            registerCodecs()
            SockJSHandler sockJSHandler = SockJSHandler.create(vertx)
            BridgeOptions tooltipBridgeOptions = new BridgeOptions()
                    .addInboundPermitted(new PermittedOptions().setAddressRegex(".+"))
                    .addOutboundPermitted(new PermittedOptions().setAddressRegex(".+"))
            sockJSHandler.bridge(tooltipBridgeOptions)

            Router router = Router.router(vertx)
            router.route("/eventbus/*").handler(sockJSHandler)
            if (config().getBoolean("bridge_ssl")) {
                vertx.createHttpServer(new HttpServerOptions().setSsl(true)
                        .setKeyStoreOptions(new JksOptions()
                        .setPath(config().getString("jks_path"))
                        .setPassword(config().getString("jks_password")))
                ).requestHandler(router.&accept)
                        .listen(config().getInteger("bridge_port"), config().getString("bridge_host"), {
                    if (it.succeeded()) {
                        log.info("Started tooltip ui bridge (using SSL). Using port: " + it.result().actualPort())
                    } else {
                        it.cause().printStackTrace()
                        log.error("Failed to start tooltip bridge (using SSL)", it.cause())
                        System.exit(-1)
                    }
                })
            } else {
                vertx.createHttpServer().requestHandler(router.&accept).listen(config().getInteger("bridge_port"),
                        config().getString("bridge_host"), {
                    if (it.succeeded()) {
                        log.info("Started tooltip ui bridge. Using port: " + it.result().actualPort())
                    } else {
                        it.cause().printStackTrace()
                        log.error("Failed to start tooltip bridge", it.cause())
                        System.exit(-1)
                    }
                })
            }

            def apiConfig = config().getJsonObject("api")
            def coreEventBus = EventBus.create(apiConfig.getString("host"), SourceTooltipConfig.current.apiBridgePort)
            coreEventBus.consumer(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, {
                def artifactMetricResult = Json.decodeValue(it.bodyAsMJson.toString(), ArtifactMetricResult.class)
                vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.address, artifactMetricResult)
            })
            coreEventBus.consumer(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, {
                def artifactTraceResult = Json.decodeValue(it.bodyAsMJson.toString(), ArtifactTraceResult.class)
                vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, artifactTraceResult)
            })

            //register any forced subscriptions
            def subscriptions = config().getJsonArray("artifact_subscriptions")
            for (int i = 0; i < subscriptions.size(); i++) {
                def sub = subscriptions.getJsonObject(i)
                if (sub.getBoolean("force_subscribe", false)) {
                    //make sure application exists first (create if necessary), then subscribe
                    coreClient.getApplication(sub.getString("app_uuid"), {
                        if (it.succeeded()) {
                            if (it.result().isPresent()) {
                                def artifactConfig = SourceArtifactConfig.builder().forceSubscribe(true).build()
                                coreClient.createArtifactConfig(sub.getString("app_uuid"),
                                        sub.getString("artifact_qualified_name"), artifactConfig, {
                                    if (it.failed()) {
                                        log.error("Failed to create artifact config", it.cause())
                                    }
                                })
                            } else {
                                def createApplication = SourceApplication.builder().isCreateRequest(true)
                                        .appUuid(sub.getString("app_uuid")).build()
                                coreClient.createApplication(createApplication, {
                                    if (it.succeeded()) {
                                        def artifactConfig = SourceArtifactConfig.builder().forceSubscribe(true).build()
                                        coreClient.createArtifactConfig(sub.getString("app_uuid"),
                                                sub.getString("artifact_qualified_name"), artifactConfig, {
                                            if (it.failed()) {
                                                log.error("Failed to create artifact config", it.cause())
                                            }
                                        })
                                    } else {
                                        log.error("Failed to create application", it.cause())
                                    }
                                })
                            }
                        } else {
                            log.error("Failed to get application", it.cause())
                        }
                    })
                }
            }
        }

        //tabs
        def overviewTabFut = Future.future()
        def tracesTabFut = Future.future()
        vertx.deployVerticle(new OverviewTab(coreClient, pluginAvailable), new DeploymentOptions()
                .setConfig(config()).setWorker(true), overviewTabFut.completer())
        vertx.deployVerticle(new TracesTab(coreClient, pluginAvailable), new DeploymentOptions()
                .setConfig(config()).setWorker(true), tracesTabFut.completer())
        CompositeFuture.all(overviewTabFut, tracesTabFut).setHandler({
            if (it.succeeded()) {
                //track
                vertx.deployVerticle(new TooltipViewTracker(), new DeploymentOptions()
                        .setWorker(true), startFuture.completer())
            } else {
                startFuture.fail(it.cause())
            }
        })

        vertx.eventBus().consumer("TooltipLogger", {
            log.info("[TOOLTIP] - " + it.body())
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void registerCodecs() {
        Json.mapper.findAndRegisterModules()
        Json.mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        Json.mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        Json.mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)

        //api
        vertx.eventBus().registerDefaultCodec(SourceApplication.class, SourceMessage.messageCodec(SourceApplication.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifact.class, SourceMessage.messageCodec(SourceArtifact.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetrics.class, SourceMessage.messageCodec(ArtifactMetrics.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetricResult.class, SourceMessage.messageCodec(ArtifactMetricResult.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifactVersion.class, SourceMessage.messageCodec(SourceArtifactVersion.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetricSubscribeRequest.class, SourceMessage.messageCodec(ArtifactMetricSubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(ArtifactTraceSubscribeRequest.class, SourceMessage.messageCodec(ArtifactTraceSubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifactUnsubscribeRequest.class, SourceMessage.messageCodec(SourceArtifactUnsubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(ArtifactTraceResult.class, SourceMessage.messageCodec(ArtifactTraceResult.class))
        vertx.eventBus().registerDefaultCodec(TraceQuery.class, SourceMessage.messageCodec(TraceQuery.class))
        vertx.eventBus().registerDefaultCodec(TraceQueryResult.class, SourceMessage.messageCodec(TraceQueryResult.class))
        vertx.eventBus().registerDefaultCodec(Trace.class, SourceMessage.messageCodec(Trace.class))
        vertx.eventBus().registerDefaultCodec(TraceSpanStackQuery.class, SourceMessage.messageCodec(TraceSpanStackQuery.class))
        vertx.eventBus().registerDefaultCodec(TraceSpanStackQueryResult.class, SourceMessage.messageCodec(TraceSpanStackQueryResult.class))
        vertx.eventBus().registerDefaultCodec(TraceSpan.class, SourceMessage.messageCodec(TraceSpan.class))
        vertx.eventBus().registerDefaultCodec(TimeFramedMetricType.class, SourceMessage.messageCodec(TimeFramedMetricType.class))
    }
}
