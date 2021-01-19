package com.sourceplusplus.portal.backend

import com.google.common.io.ByteStreams
import com.sourceplusplus.portal.PortalViewTracker
import com.sourceplusplus.portal.display.ActivityDisplay
import com.sourceplusplus.portal.display.ConfigurationDisplay
import com.sourceplusplus.portal.display.OverviewDisplay
import com.sourceplusplus.portal.display.TracesDisplay
import io.netty.buffer.Unpooled
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.nio.charset.Charset

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PortalServer(private val bridgePort: Int) : CoroutineVerticle() {

    override suspend fun start() {
        vertx.deployVerticleAwait(OverviewDisplay())
        vertx.deployVerticleAwait(ActivityDisplay())
        vertx.deployVerticleAwait(TracesDisplay())
        vertx.deployVerticleAwait(ConfigurationDisplay(false)) //todo: dynamic
        vertx.deployVerticleAwait(PortalViewTracker())

        val router = Router.router(vertx)
        router.route().handler(ResponseTimeHandler.create())

        // Static handler
        router.get("/*").handler {
            var fileStream = PortalServer::class.java.classLoader.getResourceAsStream(it.request().path())
            val response = it.response().setStatusCode(200)
            if (it.request().path() == "/" || it.request().path().endsWith(".html")) {
                fileStream = PortalServer::class.java.classLoader.getResourceAsStream("/index.html")
                response.end(
                    Buffer.buffer(
                        Unpooled.copiedBuffer(ByteStreams.toByteArray(fileStream!!)).toString(Charset.defaultCharset())
                            .replace("window.portalBridgePort = 8888", "window.portalBridgePort = $bridgePort")
                    )
                )
            } else if (it.request().path().endsWith(".js")) {
                response.putHeader("Content-Type", "text/javascript")
            }
            response.end(Buffer.buffer(Unpooled.copiedBuffer(ByteStreams.toByteArray(fileStream!!))))
            //todo: add cache headers
        }

        // Start the server
        val httpPort = vertx.sharedData().getLocalMap<String, Int>("portal")
            .getOrDefault("http.port", 0)
        val server = vertx.createHttpServer()
            .requestHandler(router)
            .listenAwait(httpPort)
        vertx.sharedData().getLocalMap<String, Int>("portal")["http.port"] = server.actualPort()
    }
}
