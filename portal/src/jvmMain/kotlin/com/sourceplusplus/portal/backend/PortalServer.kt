package com.sourceplusplus.portal.backend

import com.google.common.io.ByteStreams
import com.sourceplusplus.portal.PortalViewTracker
import com.sourceplusplus.portal.display.*
import io.netty.buffer.Unpooled
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.charset.Charset

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PortalServer(
    private val bridgePort: Int,
    private val refreshIntervalMs: Int,
    private val pullMode: Boolean
) : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(PortalServer::class.java)
    }

    override suspend fun start() {
        vertx.deployVerticle(OverviewDisplay(refreshIntervalMs, pullMode)).await()
        vertx.deployVerticle(ActivityDisplay(refreshIntervalMs, pullMode)).await()
        vertx.deployVerticle(TracesDisplay(refreshIntervalMs, pullMode)).await()
        vertx.deployVerticle(LogsDisplay(refreshIntervalMs, pullMode)).await()
        vertx.deployVerticle(ConfigurationDisplay(refreshIntervalMs, false)).await() //todo: dynamic
        vertx.deployVerticle(PortalViewTracker()).await()

        val router = Router.router(vertx)
        router.route().handler(ResponseTimeHandler.create())
        router.errorHandler(500) {
            if (it.failed()) log.error("Failed request: " + it.request().path(), it.failure())
        }

        // Static handler
        router.get("/*").handler {
            log.trace("Request: " + it.request().path() + " - Params: " + it.request().params())
            var fileStream: InputStream?
            val response = it.response().setStatusCode(200)
            if (it.request().path() == "/") {
                fileStream = PortalServer::class.java.classLoader.getResourceAsStream("index.html")
                if (fileStream == null) fileStream = PortalServer::class.java.getResourceAsStream("index.html")
                response.end(
                    Buffer.buffer(
                        Unpooled.copiedBuffer(ByteStreams.toByteArray(fileStream!!)).toString(Charset.defaultCharset())
                            .replace("window.portalBridgePort = 8888", "window.portalBridgePort = $bridgePort")
                    )
                )
            } else if (it.request().path().endsWith(".js")) {
                fileStream = PortalServer::class.java.classLoader.getResourceAsStream(it.request().path().substring(1))
                if (fileStream == null) fileStream =
                    PortalServer::class.java.getResourceAsStream(it.request().path().substring(1))
                response.putHeader("Content-Type", "text/javascript")
                    .end(Buffer.buffer(Unpooled.copiedBuffer(ByteStreams.toByteArray(fileStream!!))))
            } else {
                fileStream = PortalServer::class.java.classLoader.getResourceAsStream(it.request().path().substring(1))
                if (fileStream == null) fileStream =
                    PortalServer::class.java.getResourceAsStream(it.request().path().substring(1))
                if (fileStream != null) {
                    response.end(Buffer.buffer(Unpooled.copiedBuffer(ByteStreams.toByteArray(fileStream))))
                }
            }

            if (!response.ended()) {
                if (fileStream != null) response.statusCode = 404
                response.end()
            }
            //todo: add cache headers
        }

        // Start the server
        val httpPort = vertx.sharedData().getLocalMap<String, Int>("portal")
            .getOrDefault("http.port", 0)
        val server = vertx.createHttpServer()
            .requestHandler(router)
            .listen(httpPort).await()
        vertx.sharedData().getLocalMap<String, Int>("portal")["http.port"] = server.actualPort()
    }
}
