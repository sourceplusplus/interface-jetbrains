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
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntegrationProxy extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    public static final Set<String> ALLOWED_IP_ADDRESSES = Sets.newConcurrentHashSet()

    @Override
    void start(Future<Void> fut) throws Exception {
        //todo: no hardcoding
        vertx.createNetServer().connectHandler({ socket ->
            vertx.createNetClient().connect(11799, "localhost", { result ->
                if (result.succeeded()) {
                    log.debug("Connection request from IP address: " + socket.remoteAddress())

                    if (ALLOWED_IP_ADDRESSES.contains(socket.remoteAddress().host())) {
                        new SocketProxy(socket, result.result()).proxy()
                    } else {
                        log.warn("Rejected starting proxy for IP address: " + socket.remoteAddress())
                    }
                } else {
                    log.error(result.cause().getMessage(), result.cause())
                    socket.close()
                }
            })
        }).listen(11800, {
            if (it.succeeded()) {
                log.info("Skywalking OAP gRPC proxy started")
                fut.complete()
            } else {
                log.error("Failed to start Skywalking OAP gRPC proxy")
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
            serverSocket.closeHandler({ clientSocket.close() })
            clientSocket.closeHandler({ serverSocket.close() })
            serverSocket.exceptionHandler({
                log.error(it.getMessage(), it)
                close()
            })
            clientSocket.exceptionHandler({
                log.error(it.getMessage(), it)
                close()
            })
            clientToServerPump.start()
            serverToClientPump.start()
        }

        void close() {
            clientToServerPump.stop()
            serverToClientPump.stop()
            clientSocket.close()
            serverSocket.close()
        }
    }
}
