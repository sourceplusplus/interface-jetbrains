package com.sourceplusplus.portal

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.sourceplusplus.api.bridge.SourceBridgeClient
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.artifact.SourceArtifactVersion
import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetrics
import com.sourceplusplus.api.model.metric.TimeFramedMetricType
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import com.sourceplusplus.portal.display.PortalInterface
import com.sourceplusplus.portal.display.tabs.ConfigurationTab
import com.sourceplusplus.portal.display.tabs.OverviewTab
import com.sourceplusplus.portal.display.tabs.TracesTab
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.net.JksOptions
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.apache.commons.io.IOUtils
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout

import java.nio.charset.StandardCharsets

/**
 * Used to bootstrap the Source++ Portal.
 *
 * The portal is able to fetch and display runtime behavior without interacting with the Source++ Plugin.
 * This allows the portal to be independently outside the IDE.
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class PortalBootstrap extends AbstractVerticle {

    //todo: fix https://github.com/sourceplusplus/Assistant/issues/1 and remove static block below
    static {
        ConsoleAppender console = new ConsoleAppender()
        console.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"))
        console.setThreshold(Level.DEBUG)
        console.activateOptions()

        Logger.rootLogger.loggerRepository.resetConfiguration()
        Logger.getLogger("com.sourceplusplus").addAppender(console)
    }

    public static final MetricRegistry portalMetrics = new MetricRegistry()
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

        Vertx.vertx(vertxOptions).deployVerticle(new PortalBootstrap(false),
                new DeploymentOptions().setConfig(configJSON))
    }

    PortalBootstrap(boolean pluginAvailable) {
        this.pluginAvailable = pluginAvailable
    }

    @Override
    void start(Promise<Void> startFuture) throws Exception {
        if (pluginAvailable) {
            PortalInterface.assignVertx(vertx)
        } else {
            registerCodecs()
            SockJSHandler sockJSHandler = SockJSHandler.create(vertx)
            BridgeOptions portalBridgeOptions = new BridgeOptions()
                    .addInboundPermitted(new PermittedOptions().setAddressRegex(".+"))
                    .addOutboundPermitted(new PermittedOptions().setAddressRegex(".+"))
            sockJSHandler.bridge(portalBridgeOptions)

            Router router = Router.router(vertx)
            router.route("/eventbus/*").handler(sockJSHandler)
            if (config().getBoolean("bridge_ssl")) {
                vertx.createHttpServer(new HttpServerOptions().setSsl(true)
                        .setKeyStoreOptions(new JksOptions()
                                .setPath(config().getString("jks_path"))
                                .setPassword(config().getString("jks_password")))
                ).requestHandler(router)
                        .listen(config().getInteger("bridge_port"), config().getString("bridge_host"), {
                            if (it.succeeded()) {
                                log.info("Started portal ui bridge (using SSL). Using port: " + it.result().actualPort())
                            } else {
                                it.cause().printStackTrace()
                                log.error("Failed to start portal bridge (using SSL)", it.cause())
                                System.exit(-1)
                            }
                        })
            } else {
                vertx.createHttpServer().requestHandler(router).listen(config().getInteger("bridge_port"),
                        config().getString("bridge_host"), {
                    if (it.succeeded()) {
                        log.info("Started portal ui bridge. Using port: " + it.result().actualPort())
                    } else {
                        it.cause().printStackTrace()
                        log.error("Failed to start portal bridge", it.cause())
                        System.exit(-1)
                    }
                })
            }

            //setup connection to core
            def apiConfig = config().getJsonObject("api")
            def coreClient = new SourceCoreClient(
                    apiConfig.getString("host"), apiConfig.getInteger("port"), apiConfig.getBoolean("ssl"))
            if (apiConfig.getString("key")) {
                coreClient.setApiKey(apiConfig.getString("key"))
            }

            //setup bridge to core
            new SourceBridgeClient(vertx, apiConfig.getString("host"), apiConfig.getInteger("port"),
                    apiConfig.getBoolean("ssl")).setupSubscriptions()

            //register subscriptions
            def subscriptions = config().getJsonArray("artifact_subscriptions")
            for (int i = 0; i < subscriptions.size(); i++) {
                def sub = subscriptions.getJsonObject(i)
                def appUuid = sub.getString("app_uuid")
                def artifactQualifiedName = sub.getString("artifact_qualified_name")
                SourcePortalConfig.current.addCoreClient(appUuid, coreClient)

                if (sub.getBoolean("force_subscribe", false)) {
                    //make sure application exists first (create if necessary), then subscribe
                    SourcePortalConfig.current.getCoreClient(appUuid).getApplication(appUuid, {
                        if (it.succeeded()) {
                            if (it.result().isPresent()) {
                                def artifactConfig = SourceArtifactConfig.builder().forceSubscribe(true).build()
                                SourcePortalConfig.current.getCoreClient(appUuid).createOrUpdateArtifactConfig(appUuid, artifactQualifiedName, artifactConfig, {
                                    if (it.failed()) {
                                        log.error("Failed to create artifact config", it.cause())
                                    }
                                })
                            } else {
                                def createApplication = SourceApplication.builder().isCreateRequest(true)
                                        .appUuid(appUuid).build()
                                SourcePortalConfig.current.getCoreClient(appUuid).createApplication(createApplication, {
                                    if (it.succeeded()) {
                                        def artifactConfig = SourceArtifactConfig.builder().forceSubscribe(true).build()
                                        SourcePortalConfig.current.getCoreClient(appUuid).createOrUpdateArtifactConfig(appUuid, artifactQualifiedName, artifactConfig, {
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

                //register portal
                def portal = SourcePortal.getPortal(SourcePortal.register(appUuid, artifactQualifiedName, true))
                //keep portal active
                vertx.setPeriodic(60_000, {
                    SourcePortal.ensurePortalActive(portal)
                })
            }

            vertx.eventBus().consumer("REGISTER_PORTAL", {
                def request = it.body() as JsonObject
                def appUuid = request.getString("app_uuid")
                def artifactQualifiedName = request.getString("artifact_qualified_name")

                def activePortals = SourcePortal.getPortals(appUuid, artifactQualifiedName)
                log.info("Registering new portal")
                def portalUuid = SourcePortal.register(appUuid, artifactQualifiedName, true)
                if (activePortals) {
                    log.info("Registered new portal with cloned view from portal: " + activePortals[0].portalUuid)
                    SourcePortal.getPortal(portalUuid).interface.cloneViews(activePortals[0].interface)
                } else {
                    log.info("Registered new portal with blank view")
                }
                it.reply(new JsonObject().put("portal_uuid", portalUuid))
            })
        }

        def overviewTabFut = Promise.promise().future()
        def tracesTabFut = Promise.promise().future()
        def configurationTabFut = Promise.promise().future()
        vertx.deployVerticle(new OverviewTab(), new DeploymentOptions()
                .setConfig(config()).setWorker(true), overviewTabFut)
        vertx.deployVerticle(new TracesTab(), new DeploymentOptions()
                .setConfig(config()).setWorker(true), tracesTabFut)
        vertx.deployVerticle(new ConfigurationTab(pluginAvailable), new DeploymentOptions()
                .setConfig(config()).setWorker(true), configurationTabFut)
        CompositeFuture.all(overviewTabFut, tracesTabFut, configurationTabFut).setHandler({
            if (it.succeeded()) {
                //track
                vertx.deployVerticle(new PortalViewTracker(), new DeploymentOptions().setWorker(true), startFuture)
            } else {
                startFuture.fail(it.cause())
            }
        })

        vertx.eventBus().consumer("PortalLogger", {
            log.info("[PORTAL] - " + it.body())
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void registerCodecs() {
        DatabindCodec.mapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)

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
