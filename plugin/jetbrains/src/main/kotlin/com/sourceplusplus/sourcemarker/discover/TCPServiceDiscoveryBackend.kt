package com.sourceplusplus.sourcemarker.discover

import com.sourceplusplus.protocol.SourceMarkerServices.Provider.LOCAL_TRACING
import com.sourceplusplus.protocol.SourceMarkerServices.Provider.LOG_COUNT_INDICATOR
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetSocket
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameParser
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.spi.ServiceDiscoveryBackend
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TCPServiceDiscoveryBackend : ServiceDiscoveryBackend {

    private lateinit var vertx: Vertx
    private lateinit var client: NetClient
    private lateinit var socket: NetSocket
    private val setupPromise = Promise.promise<Void>()
    private val setupFuture = setupPromise.future()
    private val replyHandlers = ConcurrentHashMap<String, (JsonObject) -> Unit>()

    override fun init(vertx: Vertx, config: JsonObject) {
        this.vertx = vertx

        GlobalScope.launch(vertx.dispatcher()) {
            client = vertx.createNetClient()
            socket = client.connect(5455, "localhost").await()
            val parser = FrameParser { parse: AsyncResult<JsonObject> ->
                val frame = parse.result()
                assert(frame.getBoolean("send"))
                replyHandlers.remove(frame.getString("address"))!!.invoke(frame.getJsonObject("body"))
            }
            socket.handler(parser)

            setupHandler(vertx, "get-records")
            setupHandler(vertx, LOCAL_TRACING)
            setupHandler(vertx, LOG_COUNT_INDICATOR)

            setupPromise.complete()
        }
    }

    private fun setupHandler(vertx: Vertx, address: String) {
        FrameHelper.sendFrame(BridgeEventType.REGISTER.name.toLowerCase(), address, null, socket)
        vertx.eventBus().consumer<JsonObject>(address) { resultHandler ->
            val replyAddress = UUID.randomUUID().toString()
            replyHandlers[replyAddress] = {
                if (it.map.keys.size == 1 && it.containsKey("value")) {
                    resultHandler.reply(it.getValue("value"))
                } else {
                    resultHandler.reply(it)
                }
            }
            val headers = JsonObject()
            for ((key, value) in resultHandler.headers().entries()) {
                headers.put(key, value)
            }
            FrameHelper.sendFrame(
                BridgeEventType.SEND.name.toLowerCase(),
                address, replyAddress, headers, true, resultHandler.body(), socket
            )
        }
    }

    override fun store(record: Record, resultHandler: Handler<AsyncResult<Record>>) {
        TODO("Not yet implemented")
    }

    override fun remove(record: Record, resultHandler: Handler<AsyncResult<Record>>) {
        TODO("Not yet implemented")
    }

    override fun remove(uuid: String, resultHandler: Handler<AsyncResult<Record>>) {
        TODO("Not yet implemented")
    }

    override fun update(record: Record, resultHandler: Handler<AsyncResult<Void>>) {
        TODO("Not yet implemented")
    }

    override fun getRecords(resultHandler: Handler<AsyncResult<MutableList<Record>>>) {
        setupFuture.onComplete {
            vertx.eventBus().request<JsonObject>("get-records", null) {
                resultHandler.handle(Future.succeededFuture(mutableListOf(Record(it.result().body()))))
            }
        }
        if (setupFuture.isComplete) {
            vertx.eventBus().request<JsonObject>("get-records", null) {
                resultHandler.handle(Future.succeededFuture(mutableListOf(Record(it.result().body()))))
            }
        }
    }

    override fun getRecord(uuid: String, resultHandler: Handler<AsyncResult<Record>>) {
        TODO("Not yet implemented")
    }

    override fun name() = "tcp-service-discovery"
}
