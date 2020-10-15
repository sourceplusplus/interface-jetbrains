package com.sourceplusplus.portal.backend

import com.sourceplusplus.portal.PortalViewTracker
import com.sourceplusplus.portal.display.ActivityDisplay
import com.sourceplusplus.portal.display.ConfigurationDisplay
import com.sourceplusplus.portal.display.TracesDisplay
import io.netty.buffer.Unpooled
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.ResponseTimeHandler
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PortalServer : CoroutineVerticle() {

    override suspend fun start() {
        vertx.deployVerticleAwait(ActivityDisplay())
        vertx.deployVerticleAwait(TracesDisplay())
        vertx.deployVerticleAwait(ConfigurationDisplay(false)) //todo: dynamic
        vertx.deployVerticleAwait(PortalViewTracker())

        val router = Router.router(vertx)
        router.route().handler(ResponseTimeHandler.create())

//        // Static handler
//        router.route("/*").handler(StaticHandler.create())

        // Static handler
        router.get("/*").handler {
            val fileStream = PortalServer::class.java.classLoader.getResourceAsStream(it.request().path())
            val response = it.response().setStatusCode(200)
            if (it.request().path().endsWith(".js")) {
                response.putHeader("Content-Type", "text/javascript")
            }
            response.end(Buffer.buffer(Unpooled.copiedBuffer(fileStream!!.readAllBytes())))
            //todo: add cache headers
        }

        // Start the server
        vertx.createHttpServer()
            .requestHandler(router)
            .listenAwait(config.getInteger("http.port", 8080))
    }
}
