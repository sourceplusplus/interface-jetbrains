package com.sourceplusplus.core

import com.sourceplusplus.api.model.SourceMessage
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.config.SourceCoreConfig
import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.info.SourceCoreInfo
import com.sourceplusplus.api.model.integration.ConnectionType
import com.sourceplusplus.api.model.integration.IntegrationConnection
import com.sourceplusplus.core.integration.IntegrationProxy
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
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
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions
import io.vertx.ext.web.handler.sockjs.SockJSSocket
import io.vertx.ext.web.handler.sockjs.impl.EventBusBridgeImpl
import io.vertx.ext.web.handler.sockjs.impl.SockJSHandlerImpl

import java.time.Instant

/**
 * Setup the Source++ Core server.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SourceCoreServer extends AbstractVerticle {

    public static ClassLoader RESOURCE_LOADER = SourceCoreServer.class.getClassLoader()

    private final JsonObject serverConfig
    private final String version
    private final Instant buildDate
    private String listenHost
    private int listenPort

    SourceCoreServer(JsonObject serverConfig, String version, Instant buildDate) {
        this.serverConfig = Objects.requireNonNull(serverConfig)
        this.version = Objects.requireNonNull(version)
        this.buildDate = buildDate
    }

    @Override
    void start(Promise<Void> startFuture) throws Exception {
        SourceMessage.registerCodecs(vertx)

        def baseRouter = Router.router(vertx)
        baseRouter.route().failureHandler(createFailureHandler())
        baseRouter.route().consumes("application/json").handler(BodyHandler.create())
        baseRouter.route().produces("application/json").handler(ResponseContentTypeHandler.create())

        def v1ApiRouter = Router.router(vertx)
        baseRouter.mountSubRouter("/v1", v1ApiRouter)
        def core = new SourceCore(v1ApiRouter)

        //start bridge
        log.info("Booting Source++ Core eventbus bridge...")
        SockJSHandler sock = new SockJSHandlerImpl(vertx, new SockJSHandlerOptions()) {
            Router bridge(SockJSBridgeOptions bridgeOptions, Handler<BridgeEvent> bridgeEventHandler) {
                return socketHandler(new EventBusBridgeImpl(vertx, bridgeOptions, bridgeEventHandler) {
                    void deliverMessage(SockJSSocket sock, String address, Message message) {
                        if (message."sentBody" != null) {
                            message."sentBody" = new JsonObject(Json.encode(message."sentBody"))
                        }
                        if (message."receivedBody" != null) {
                            message."receivedBody" = new JsonObject(Json.encode(message."receivedBody"))
                        }
                        super.deliverMessage(sock, address, message)
                    }
                })
            }
        }
        sock.bridge(new SockJSBridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex("public-events\\..+"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("public-events\\..+")))
        baseRouter.route("/eventbus/*").handler(sock)

        //optional API auth
        if (serverConfig.getJsonObject("core").getBoolean("secure_mode")) {
            log.info("Using secure mode")
            SourceCoreConfig.current.secureApi = true
            def provider = JWTAuth.create(vertx, new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("HS256")
                    .setPublicKey(serverConfig.getJsonObject("core").getString("api_key"))
                    .setSymmetric(true)))

            v1ApiRouter.route("/*").handler(JWTAuthHandler.create(provider))
            baseRouter.get("/newToken").handler({ ctx ->
                def apiKey = ctx.queryParam("key").get(0)
                if (apiKey == serverConfig.getJsonObject("core").getString("api_key")) {
                    ctx.response().putHeader("Content-Type", "text/plain")
                    ctx.response().end(provider.generateToken(new JsonObject(), new JWTOptions()))
                } else {
                    ctx.response().setStatusCode(401)
                            .end(Json.encode(new SourceAPIError().addError("Invalid key")))
                }
            })

            vertx.deployVerticle(new IntegrationProxy(), new DeploymentOptions().setConfig(serverConfig))
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
            it.response().setStatusCode(200).end(Json.encode(getSourceCoreInfo(core)))
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
        def serverOptions = createSeverOptions()
        def server = vertx.createHttpServer(serverOptions)
        server.requestHandler(baseRouter).listen({
            if (it.succeeded()) {
                listenHost = serverOptions.host
                listenPort = it.result().actualPort()
                log.info("Source++ Core listening on: $listenHost:$listenPort")

                vertx.deployVerticle(core, new DeploymentOptions().setConfig(serverConfig), {
                    if (it.succeeded()) {
                        enableSelfMonitoring(core)
                        log.info("Source++ Core online!")
                        startFuture.complete()
                    } else {
                        startFuture.fail(it.cause())
                    }
                })
            } else {
                startFuture.fail(it.cause())
            }
        })
    }

    @Override
    void stop() throws Exception {
        log.info("{} stopped", getClass().getSimpleName())
    }

    String getHost() {
        return listenHost
    }

    int getPort() {
        return listenPort
    }

    private HttpServerOptions createSeverOptions() {
        def options = new HttpServerOptions()
        def coreConfig = serverConfig.getJsonObject("core")
        options.setHost(coreConfig.getString("host"))
        options.setPort(coreConfig.getInteger("port"))
        if (coreConfig.getBoolean("ssl")) {
            def jksOptions = new JksOptions()
            jksOptions.setPath(coreConfig.getString("jks_path"))
            jksOptions.setPassword(coreConfig.getString("jks_password"))

            options.ssl = true
            options.keyStoreOptions = jksOptions
        }
        return options
    }

    private static void enableSelfMonitoring(SourceCore core) {
        try {
            Class.forName("org.apache.skywalking.apm.agent.SkyWalkingAgent")
        } catch (ClassNotFoundException ignore) {
            log.info("Self monitoring disabled")
            return
        }

        def sourceCoreApplication = SourceApplication.builder()
                .appUuid("99999999-9999-9999-9999-999999999999")
                .appName("source-core").isCreateRequest(true).build()
        core.applicationAPI.createApplication(sourceCoreApplication, {
            if (it.succeeded()) {
                log.info("Self monitoring enabled")
            } else {
                log.error("Failed to create application for self monitoring", it.cause())
            }
        })
    }

    private SourceCoreInfo getSourceCoreInfo(SourceCore core) {
        def activeIntegrations = core.getActiveIntegrations()
        def publicActiveIntegrations = []
        activeIntegrations.each {
            def updatedConnections = new HashMap<ConnectionType, IntegrationConnection>(it.connections())
            updatedConnections.each {
                def port = it.value.port
                def publicPort = vertx.sharedData().getLocalMap("integration.proxy")
                        .getOrDefault(port.toString(), port) as int
                updatedConnections.put(it.key, it.value.withPort(publicPort).withProxyPort(null))
            }
            publicActiveIntegrations += it.withConnections(updatedConnections)
        }

        return SourceCoreInfo.builder().version(version).buildDate(buildDate)
                .config(SourceCoreConfig.current).activeIntegrations(publicActiveIntegrations).build()
    }

    private static Handler<RoutingContext> createFailureHandler() {
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
}
