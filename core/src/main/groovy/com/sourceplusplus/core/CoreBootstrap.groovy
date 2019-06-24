package com.sourceplusplus.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.artifact.SourceArtifactVersion
import com.sourceplusplus.api.model.config.SourceAgentConfig
import com.sourceplusplus.api.model.config.SourceCoreConfig
import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.info.SourceCoreInfo
import com.sourceplusplus.api.model.integration.ConnectionType
import com.sourceplusplus.api.model.integration.IntegrationConnection
import com.sourceplusplus.api.model.internal.ApplicationArtifact
import com.sourceplusplus.api.model.metric.*
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest
import com.sourceplusplus.api.model.trace.ArtifactTraceUnsubscribeRequest
import com.sourceplusplus.api.model.trace.TraceSpan
import com.sourceplusplus.core.integration.IntegrationProxy
import io.vertx.core.*
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.net.JksOptions
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.jwt.JWTOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.apache.commons.io.IOUtils
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.time.Instant

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE

/**
 * todo: description
 *
 * @version 0.2.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class CoreBootstrap extends AbstractVerticle {

    public static final ResourceBundle BUILD = ResourceBundle.getBundle("source-core_build")

    private static final Logger log = LoggerFactory.getLogger(this.name)

    static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")

        def configJSON
        if (args.length > 0) {
            log.info("Using configuration passed via program arguments")
            configJSON = new JsonObject(args[0])
        } else {
            def configFile = System.getenv("SOURCE_CONFIG")
            if (!configFile) {
                log.warn("Missing SOURCE_CONFIG environment variable. Using default settings")
                configFile = "local.json"
            }

            log.info("Using configuration file: $configFile")
            def configInputStream
            if (new File(configFile).exists()) {
                configInputStream = new File(configFile).newInputStream()
            } else {
                configInputStream = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("config/$configFile")
            }
            def configData = IOUtils.toString(configInputStream, StandardCharsets.UTF_8)
            configJSON = new JsonObject(configData)
        }

        def vertxOptions = new VertxOptions()
        if (BUILD.getString("version") == "dev") {
            //allow debug pauses
            vertxOptions.setBlockedThreadCheckInterval(Integer.MAX_VALUE)
        }
        log.info("Build: " + BUILD.getString("build_date"))
        Vertx.vertx(vertxOptions).deployVerticle(new CoreBootstrap(), new DeploymentOptions().setConfig(configJSON))
    }

    @SuppressWarnings("unused")
    static void boot(Vertx vertx, Handler<AsyncResult<String>> completionHandler) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
        def configJSON
        def configFile = System.getenv("SOURCE_CONFIG")
        if (!configFile) {
            throw new RuntimeException("Missing SOURCE_CONFIG system environment!")
        }

        log.info("Using configuration file: $configFile")
        def configInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile)
        def configData = IOUtils.toString(configInputStream, StandardCharsets.UTF_8)
        configJSON = new JsonObject(configData)
        vertx.deployVerticle(new CoreBootstrap(), new DeploymentOptions().setConfig(configJSON), completionHandler)
    }

    private CoreBootstrap() {
        //no instances; use main or boot
    }

    @Override
    void start(Future<Void> startFuture) throws Exception {
        registerCodecs()

        def baseRouter = createRouter()
        def v1ApiRouter = Router.router(vertx)
        baseRouter.mountSubRouter("/v1", v1ApiRouter)
        def core = new SourceCore(v1ApiRouter)

        //start bridge
        log.info("Booting Source++ Core eventbus bridge...")
        SockJSHandler sock = SockJSHandler.create(vertx)
        sock.bridge(new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex("public-events\\..+"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("public-events\\..+")))
        baseRouter.route("/eventbus/*").handler(sock)

        //optional API auth
        if (config().getJsonObject("core").getBoolean("secure_mode")) {
            log.info("Using secure mode")
            SourceCoreConfig.current.secureApi = true
            def provider = JWTAuth.create(vertx, new JWTAuthOptions()
                    .addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("HS256")
                    .setPublicKey(config().getJsonObject("core").getString("api_key"))
                    .setSymmetric(true)))

            v1ApiRouter.route("/*").handler(JWTAuthHandler.create(provider))
            baseRouter.get("/newToken").handler({ ctx ->
                def apiKey = ctx.queryParam("key").get(0)
                if (apiKey == config().getJsonObject("core").getString("api_key")) {
                    ctx.response().putHeader("Content-Type", "text/plain")
                    ctx.response().end(provider.generateToken(new JsonObject(), new JWTOptions()))
                } else {
                    ctx.response().setStatusCode(401)
                            .end(Json.encode(new SourceAPIError().addError("Invalid key")))
                }
            })

            vertx.deployVerticle(new IntegrationProxy(), new DeploymentOptions().setConfig(config()))
        } else {
            SourceCoreConfig.current.secureApi = false
        }

        if (SourceCoreConfig.current.pingEndpointAvailable) {
            //for connection testing
            baseRouter.get("/ping").handler({
                it.response().setStatusCode(200).end(new JsonObject().put("status", "ok").toString())
            })
        }

        //general info (version, etc)
        v1ApiRouter.get("/info").handler({
            it.response().setStatusCode(200).end(Json.encode(getSourceCoreInfo(core, true)))
        })
        v1ApiRouter.get("/registerIP").handler({
            def ipAddress = it.request().remoteAddress().host()
            if (IntegrationProxy.ALLOWED_IP_ADDRESSES.add(ipAddress)) {
                log.info("Registered IP address: " + ipAddress)
            }
            it.response().setStatusCode(200)
                    .end(Json.encode(new JsonArray(IntegrationProxy.ALLOWED_IP_ADDRESSES.asList())))
        })

        //start core HTTP server
        log.info("Booting Source++ Core HTTP server...")
        def server = vertx.createHttpServer(createSeverOptions())
        server.requestHandler(baseRouter.&accept)
        server.listen({
            if (it.succeeded()) {
                vertx.deployVerticle(core, new DeploymentOptions().setConfig(config()), {
                    if (it.succeeded()) {
                        enableSelfMonitoring(core)
                        log.info("Source++ Core online!")
                        startFuture.complete()
                    } else {
                        startFuture.fail(it.cause())
                        vertx.close({
                            System.exit(-1)
                        })
                    }
                })
            } else {
                startFuture.fail(it.cause())
                vertx.close({
                    System.exit(-1)
                })
            }
        })
    }

    @Override
    void stop() throws Exception {
        log.info("{} stopped", getClass().getSimpleName())
    }

    @NotNull
    private HttpServerOptions createSeverOptions() {
        def options = new HttpServerOptions()
        def coreConfig = config().getJsonObject("core")
        options.setHost(coreConfig.getString("host"))
        options.setPort(coreConfig.getInteger("port"))
        if (coreConfig.getBoolean("ssl")) {
            def jksOptions = new JksOptions()
            jksOptions.setPath(coreConfig.getString("jks_path"))
            jksOptions.setPassword(coreConfig.getString("jks_password"))

            options.ssl = true
            options.keyStoreOptions = jksOptions
        }

        log.info("Source++ Core listening on: $options.host:$options.port")
        return options
    }

    @NotNull
    private Router createRouter() {
        def router = Router.router(vertx)
        router.route().failureHandler(getFailureHandler())

        //consume/produce JSON only
        router.route().consumes("application/json")
        router.route().produces("application/json")

        //enable the body parser so we can handle JSON input
        router.route().handler(BodyHandler.create())

        //ensure no matter what responses are returned as JSON
        router.route().handler({ context ->
            context.response().headers().add(CONTENT_TYPE, "application/json")
            context.next()
        })
        return router
    }

    private void enableSelfMonitoring(SourceCore core) {
        Class agentClass
        try {
            agentClass = Class.forName("com.sourceplusplus.agent.SourceAgent")
        } catch (ClassNotFoundException ex) {
            log.info("Self monitoring disabled")
            return
        }

        def sourceCoreApplication = SourceApplication.builder()
                .appName("source-core").isCreateRequest(true).build()
        core.applicationAPI.createApplication(sourceCoreApplication, {
            if (it.succeeded()) {
                def application = it.result()
                def agentConfig = new SourceAgentConfig()
                agentConfig.appUuid = application.appUuid()

                vertx.executeBlocking({
                    try {
                        agentClass.getMethod("overrideSourceAgentConfig", SourceAgentConfig.class)
                                .invoke(null, agentConfig)
                        agentClass.getMethod("startArtifactTraceSubscriptionSync").invoke(null)
                        agentClass.getMethod("bootIntegrations", SourceCoreInfo.class)
                                .invoke(null, getSourceCoreInfo(core, false))
                        it.complete()
                    } catch (all) {
                        it.fail(all)
                    }
                }, false, {
                    if (it.succeeded()) {
                        log.info("Self monitoring enabled")
                    } else {
                        log.error("Failed to enable self monitoring", it.cause())
                    }
                })
            } else {
                log.error("Failed to create application for self monitoring", it.cause())
            }
        })
    }

    private SourceCoreInfo getSourceCoreInfo(SourceCore core, boolean resolveProxyPorts) {
        def version = BUILD.getString("version")
        def activeIntegrations = core.getActiveIntegrations()
        def publicActiveIntegrations = []
        activeIntegrations.each {
            def updatedConnections = new HashMap<ConnectionType, IntegrationConnection>(it.connections())
            if (resolveProxyPorts) {
                updatedConnections.each {
                    def port = it.value.port
                    def publicPort = vertx.sharedData().getLocalMap("integration.proxy")
                            .getOrDefault(port.toString(), port) as int
                    updatedConnections.put(it.key, it.value.withPort(publicPort).withProxyPort(null))
                }
            }
            publicActiveIntegrations += it.withConnections(updatedConnections)
        }

        def coreInfo = SourceCoreInfo.builder()
                .version(version)
                .config(SourceCoreConfig.current)
                .activeIntegrations(publicActiveIntegrations)
        if (version != "dev") {
            coreInfo.buildDate(Instant.parse(BUILD.getString("build_date")))
        }
        return coreInfo.build()
    }

    @NotNull
    private static Handler<RoutingContext> getFailureHandler() {
        return {
            RoutingContext ctx ->
                if (ctx.statusCode() == 401) {
                    ctx.response().setStatusCode(401)
                            .end(Json.encode(new SourceAPIError().addError("Unauthorized access")))
                } else {
                    def errorMessage = new SourceAPIError().addError(ctx.failure().message)
                    ctx.response().setStatusCode(500).end(Json.encode(errorMessage))
                    ctx.failure().printStackTrace()
                }
        }
    }

    private void registerCodecs() {
        Json.mapper.findAndRegisterModules()
        Json.mapper.registerModule(new GuavaModule())
        Json.mapper.registerModule(new Jdk8Module())
        Json.mapper.registerModule(new JavaTimeModule())
        Json.mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        Json.mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        Json.mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)

        //api
        vertx.eventBus().registerDefaultCodec(SourceCoreInfo.class, SourceMessage.messageCodec(SourceCoreInfo.class))
        vertx.eventBus().registerDefaultCodec(SourceApplication.class, SourceMessage.messageCodec(SourceApplication.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifactVersion.class, SourceMessage.messageCodec(SourceArtifactVersion.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetrics.class, SourceMessage.messageCodec(ArtifactMetrics.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetricResult.class, SourceMessage.messageCodec(ArtifactMetricResult.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifactConfig.class, SourceMessage.messageCodec(SourceArtifactConfig.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifact.class, SourceMessage.messageCodec(SourceArtifact.class))
        vertx.eventBus().registerDefaultCodec(ApplicationArtifact.class, SourceMessage.messageCodec(ApplicationArtifact.class))
        vertx.eventBus().registerDefaultCodec(TraceSpan.class, SourceMessage.messageCodec(TraceSpan.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetricSubscribeRequest.class, SourceMessage.messageCodec(ArtifactMetricSubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(ArtifactMetricUnsubscribeRequest.class, SourceMessage.messageCodec(ArtifactMetricUnsubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(ArtifactTraceSubscribeRequest.class, SourceMessage.messageCodec(ArtifactTraceSubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(ArtifactTraceUnsubscribeRequest.class, SourceMessage.messageCodec(ArtifactTraceUnsubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(SourceArtifactUnsubscribeRequest.class, SourceMessage.messageCodec(SourceArtifactUnsubscribeRequest.class))
        vertx.eventBus().registerDefaultCodec(TimeFramedMetricType.class, SourceMessage.messageCodec(TimeFramedMetricType.class))
    }
}
