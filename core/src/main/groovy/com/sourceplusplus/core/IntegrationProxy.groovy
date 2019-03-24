package com.sourceplusplus.core

import com.google.common.collect.Sets
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.net.NetSocket
import io.vertx.core.streams.Pump
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.1.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntegrationProxy extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    public static final Set<String> ALLOWED_IP_ADDRESSES = Sets.newConcurrentHashSet()

    @Override
    void start(Future<Void> fut) throws Exception {
        //todo: no hardcoding
        vertx.createNetServer().connectHandler({ clientSocket ->
            vertx.createNetClient().connect(11799, "localhost", { serverSocket ->
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
        }).listen(11800, {
            if (it.succeeded()) {
                log.info("SkyWalking OAP gRPC proxy started")
                fut.complete()
            } else {
                log.error("Failed to start SkyWalking OAP gRPC proxy")
                it.cause().printStackTrace()
                System.exit(-1)
            }
        })
        log.info("{} started", getClass().getSimpleName())
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
