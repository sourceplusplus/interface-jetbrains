package com.sourceplusplus.core.integration

import com.google.common.collect.Sets
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.core.streams.Pump
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.2.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntegrationProxy extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    public static final Set<String> ALLOWED_IP_ADDRESSES = Sets.newConcurrentHashSet()

    @Override
    void start(Future<Void> fut) throws Exception {
        def proxyIntegrationFutures = []
        def integrations = config().getJsonArray("integrations")
        for (int i = 0; i < integrations.size(); i++) {
            def integration = integrations.getJsonObject(i)
            def connections = integration.getJsonObject("connections")
            for (def connection : connections) {
                def conn = connection.value as JsonObject
                def proxyPort = conn.getInteger("proxy_port")
                if (proxyPort) {
                    def actualPort = conn.getInteger("port")
                    def proxyFuture = Future.future()
                    proxyIntegrationFutures += proxyFuture
                    vertx.createNetServer().connectHandler({ clientSocket ->
                        vertx.createNetClient().connect(actualPort, "localhost", { serverSocket ->
                            if (serverSocket.succeeded()) {
                                log.debug("Connection request from IP address: " + clientSocket.remoteAddress())

                                if (ALLOWED_IP_ADDRESSES.contains(clientSocket.remoteAddress().host())) {
                                    new SocketProxy(clientSocket, serverSocket.result()).proxy()
                                } else {
                                    log.warn("Rejected starting proxy for IP address: " + clientSocket.remoteAddress())
                                }
                            } else {
                                log.error(serverSocket.cause().getMessage(), serverSocket.cause())
                                clientSocket.close()
                            }
                        })
                    }).listen(proxyPort, {
                        if (it.succeeded()) {
                            log.info("Started integration proxy. Proxy port: $proxyPort - Actual port: $actualPort")
                            vertx.sharedData().getLocalMap("integration.proxy")
                                    .put(proxyPort.toString(), actualPort)
                            proxyFuture.complete()
                        } else {
                            proxyFuture.fail(new IllegalStateException(
                                    "Failed to start integration proxy on port: " + proxyPort))
                        }
                    })
                }
            }
        }
        CompositeFuture.all(proxyIntegrationFutures).setHandler({
            if (it.succeeded()) {
                fut.complete()
                log.info("IntegrationProxy started", getClass().getSimpleName())
            } else {
                fut.fail(new IllegalStateException("Failed to start SkyWalking OAP gRPC proxy"))
            }
        })
    }

    private class SocketProxy {

        private final NetSocket clientSocket
        private final NetSocket serverSocket
        private final Pump clientToServerPump
        private final Pump serverToClientPump

        SocketProxy(NetSocket clientSocket, NetSocket serverSocket) {
            this.clientSocket = clientSocket
            this.serverSocket = serverSocket
            this.clientToServerPump = Pump.pump(clientSocket, serverSocket)
            this.serverToClientPump = Pump.pump(serverSocket, clientSocket)
        }

        void proxy() {
            log.info("TCP proxy established. Client: {} - Server: {}",
                    clientSocket.remoteAddress(), serverSocket.remoteAddress())
            serverSocket.closeHandler({
                log.info("Server closed proxy connection")
                clientToServerPump.stop()
                serverToClientPump.stop()
                clientSocket.close()
            })
            clientSocket.closeHandler({
                log.info("Client closed proxy connection")
                serverToClientPump.stop()
                serverToClientPump.stop()
                serverSocket.close()
            })
            serverSocket.exceptionHandler({
                log.error("Server threw error: " + it.getMessage(), it)
            })
            clientSocket.exceptionHandler({
                log.error("Client threw error: " + it.getMessage(), it)
            })
            clientToServerPump.start()
            serverToClientPump.start()
        }
    }
}
