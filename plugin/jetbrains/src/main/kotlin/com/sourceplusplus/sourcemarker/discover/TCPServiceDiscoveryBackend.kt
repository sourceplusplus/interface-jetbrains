package com.sourceplusplus.sourcemarker.discover

import com.sourceplusplus.protocol.SourcePlusPlusServices.LOG_COUNT_INDICATOR
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
import io.vertx.servicediscovery.ServiceDiscoveryOptions
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

    private lateinit var client: NetClient
    private lateinit var socket: NetSocket
    private val setupPromise = Promise.promise<Void>()
    private val setupFuture = setupPromise.future()
    private val replyHandlers = ConcurrentHashMap<String, (JsonObject) -> Unit>()

    override fun init(vertx: Vertx, config: JsonObject) {
        GlobalScope.launch(vertx.dispatcher()) {
            client = vertx.createNetClient()
            socket = client.connect(5455, "localhost").await()
            val parser = FrameParser { parse: AsyncResult<JsonObject> ->
                val frame = parse.result()
                assert(frame.getBoolean("send"))
                replyHandlers.remove(frame.getString("address"))!!.invoke(frame.getJsonObject("body"))
            }
            socket.handler(parser)
            FrameHelper.sendFrame(BridgeEventType.REGISTER.name.toLowerCase(), "get-records", null, socket)
            FrameHelper.sendFrame(BridgeEventType.REGISTER.name.toLowerCase(), LOG_COUNT_INDICATOR, null, socket)
            vertx.eventBus().consumer<JsonObject>(LOG_COUNT_INDICATOR) { resultHandler ->
                val replyAddress = UUID.randomUUID().toString()
                replyHandlers[replyAddress] = {
                    resultHandler.reply(it.getValue("value"))
                }
                val headers = JsonObject()
                for ((key, value) in resultHandler.headers().entries()) {
                    headers.put(key, value)
                }
                FrameHelper.sendFrame(
                    BridgeEventType.SEND.name.toLowerCase(),
                    LOG_COUNT_INDICATOR, replyAddress, headers, true, resultHandler.body(), socket
                )
            }
            setupPromise.complete()
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
            val replyAddress = UUID.randomUUID().toString()
            replyHandlers[replyAddress] = {
                resultHandler.handle(Future.succeededFuture(mutableListOf(Record(it))))
            }
            FrameHelper.sendFrame(
                BridgeEventType.SEND.name.toLowerCase(),
                "get-records",
                replyAddress,
                null,
                socket
            )
        }
        if (setupFuture.isComplete) {
            TODO()
        }
    }

    override fun getRecord(uuid: String, resultHandler: Handler<AsyncResult<Record>>) {
        TODO("Not yet implemented")
    }

    override fun name() = "tcp-service-discovery"
}
