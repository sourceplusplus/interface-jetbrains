package com.sourceplusplus.plugin

import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.config.SourcePortalConfig
import groovy.util.logging.Slf4j
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.jetbrains.annotations.NotNull

/**
 * Used to bootstrap the Source++ Plugin.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SourcePlugin {

    public static final ResourceBundle BUILD = ResourceBundle.getBundle(
            "source-plugin_build", Locale.default, SourcePlugin.classLoader)

    private static Vertx vertx
    private PluginBootstrap pluginBootstrap

    SourcePlugin(SourceCoreClient coreClient) {
        if (vertx == null) vertx = Vertx.vertx()
        System.addShutdownHook {
            vertx.close()
        }
        updateEnvironment(Objects.requireNonNull(coreClient))
        vertx.deployVerticle(pluginBootstrap = new PluginBootstrap(this))

        //start plugin bridge for portal
        startPortalUIBridge({
            if (it.failed()) {
                log.error("Failed to start portal ui bridge", it.cause())
                throw new RuntimeException(it.cause())
            } else {
                log.info("PluginBootstrap started")
                SourcePortalConfig.current.pluginUIPort = it.result().actualPort()
                log.info("Using portal ui bridge port: " + SourcePortalConfig.current.pluginUIPort)
            }
        })
    }

    static void updateEnvironment(SourceCoreClient coreClient) {
        SourcePluginConfig.current.activeEnvironment.coreClient = coreClient
        coreClient.attachBridge(vertx)
        if (SourcePluginConfig.current.activeEnvironment.appUuid) {
            SourcePortalConfig.current.addCoreClient(SourcePluginConfig.current.activeEnvironment.appUuid, coreClient)
        }
    }

    private static void startPortalUIBridge(Handler<AsyncResult<HttpServer>> listenHandler) {
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx)
        SockJSBridgeOptions portalBridgeOptions = new SockJSBridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex(".+"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(".+"))
        sockJSHandler.bridge(portalBridgeOptions)

        Router router = Router.router(vertx)
        router.route("/eventbus/*").handler(sockJSHandler)
        vertx.createHttpServer().requestHandler(router).listen(0, listenHandler)
    }

    @NotNull
    static Vertx getVertx() {
        return vertx
    }

    static void setVertx(Vertx vertx) {
        this.vertx = vertx
    }
}
