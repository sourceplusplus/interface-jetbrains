/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.sourcemarker.discover

import eu.geekplace.javapinning.JavaPinning
import eu.geekplace.javapinning.pin.Pin
import io.vertx.core.*
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.NetSocket
import io.vertx.core.net.TrustOptions
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameParser
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.spi.ServiceDiscoveryBackend
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.jetbrains.sourcemarker.settings.isSsl
import spp.jetbrains.sourcemarker.settings.serviceHostNormalized
import spp.protocol.SourceServices.Utilize
import spp.protocol.extend.TCPServiceFrameParser
import spp.protocol.platform.PlatformAddress
import spp.protocol.platform.status.InstanceConnection
import java.util.*

/**
 * todo: description.
 *
 * @since 0.2.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class TCPServiceDiscoveryBackend : ServiceDiscoveryBackend {

    companion object {
        private val log = LoggerFactory.getLogger(TCPServiceDiscoveryBackend::class.java)
        var socket: NetSocket? = null
    }

    private lateinit var vertx: Vertx
    private lateinit var client: NetClient
    private lateinit var pluginConfig: SourceMarkerConfig
    private val setupPromise = Promise.promise<Void>()
    private val setupFuture = setupPromise.future()

    override fun init(vertx: Vertx, config: JsonObject) {
        this.vertx = vertx
        pluginConfig = Json.decodeValue(
            config.getJsonObject("sourcemarker_plugin_config").toString(), SourceMarkerConfig::class.java
        )

        val serviceHost = pluginConfig.serviceHostNormalized!!
        val hardcodedConfig = config.getJsonObject("hardcoded_config")
        val servicePort = hardcodedConfig.getInteger("tcp_service_port")
        val certificatePins = mutableListOf<String>()
        certificatePins.addAll(pluginConfig.certificatePins)
        val hardcodedPin = hardcodedConfig.getString("certificate_pin")
        if (!hardcodedPin.isNullOrBlank()) {
            certificatePins.add(hardcodedPin)
        }

        GlobalScope.launch(vertx.dispatcher()) {
            try {
                client = if (certificatePins.isNotEmpty()) {
                    val options = NetClientOptions()
                        .setReconnectAttempts(Int.MAX_VALUE).setReconnectInterval(5000)
                        .setSsl(pluginConfig.isSsl())
                        .setTrustOptions(
                            TrustOptions.wrap(
                                JavaPinning.trustManagerForPins(
                                    certificatePins.map { Pin.fromString("CERTSHA256:$it") }
                                )
                            )
                        )
                    vertx.createNetClient(options)
                } else {
                    val options = NetClientOptions()
                        .setReconnectAttempts(Int.MAX_VALUE).setReconnectInterval(5000)
                        .setSsl(pluginConfig.isSsl())
                        .apply {
                            if (!pluginConfig.verifyHost) {
                                isTrustAll = true
                            }
                        }
                    vertx.createNetClient(options)
                }
                socket = client.connect(servicePort, serviceHost).await()
            } catch (ex: Exception) {
                log.error("Failed to connect to service discovery server", ex)
                setupPromise.fail(ex)
                return@launch
            }
            socket!!.handler(FrameParser(TCPServiceFrameParser(vertx, socket!!)))

            vertx.executeBlocking<Any> {
                setupHandler(vertx, "get-records")
                setupHandler(vertx, Utilize.LIVE_SERVICE)
                setupHandler(vertx, Utilize.LIVE_INSTRUMENT)
                setupHandler(vertx, Utilize.LIVE_VIEW)
                setupHandler(vertx, Utilize.LOG_COUNT_INDICATOR)

                //setup connection
                val replyAddress = UUID.randomUUID().toString()
                val pc = InstanceConnection(UUID.randomUUID().toString(), System.currentTimeMillis())
                val consumer: MessageConsumer<Boolean> = vertx.eventBus().localConsumer(replyAddress)

                val promise = Promise.promise<Void>()
                consumer.handler {
                    //todo: handle false
                    if (it.body() == true) {
                        promise.complete()
                        consumer.unregister()
                        setupPromise.complete()
                    }
                }
                val headers = JsonObject().apply { pluginConfig.serviceToken?.let { put("auth-token", it) } }
                FrameHelper.sendFrame(
                    BridgeEventType.SEND.name.toLowerCase(),
                    PlatformAddress.MARKER_CONNECTED,
                    replyAddress, headers, true, JsonObject.mapFrom(pc), socket!!
                )
            }
        }
    }

    private fun setupHandler(vertx: Vertx, address: String) {
        vertx.eventBus().localConsumer<JsonObject>(address) { resp ->
            val replyAddress = UUID.randomUUID().toString()
            val tempConsumer = vertx.eventBus().localConsumer<Any>(replyAddress)
            tempConsumer.handler {
                resp.reply(it.body())
                tempConsumer.unregister()
            }

            val headers = JsonObject()
            resp.headers().entries().forEach { headers.put(it.key, it.value) }
            FrameHelper.sendFrame(
                BridgeEventType.SEND.name.toLowerCase(),
                address, replyAddress, headers, true, resp.body(), socket!!
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
        if (setupFuture.isComplete) {
            if (setupFuture.succeeded()) {
                val deliveryOptions = DeliveryOptions()
                    .apply { pluginConfig.serviceToken?.let { addHeader("auth-token", it) } }
                vertx.eventBus().request<JsonObject>("get-records", null, deliveryOptions) {
                    resultHandler.handle(Future.succeededFuture(mutableListOf(Record(it.result().body()))))
                }
            } else {
                resultHandler.handle(Future.failedFuture(setupFuture.cause()))
            }
        } else {
            setupFuture.onComplete {
                if (it.succeeded()) {
                    val deliveryOptions = DeliveryOptions()
                        .apply { pluginConfig.serviceToken?.let { addHeader("auth-token", it) } }
                    vertx.eventBus().request<JsonArray>("get-records", null, deliveryOptions) {
                        val records = mutableListOf<Record>()
                        it.result().body().forEach { record ->
                            records.add(Record(record as JsonObject))
                        }
                        resultHandler.handle(Future.succeededFuture(records))
                    }
                } else {
                    resultHandler.handle(Future.failedFuture(it.cause()))
                }
            }
        }
    }

    override fun getRecord(uuid: String, resultHandler: Handler<AsyncResult<Record>>) {
        TODO("Not yet implemented")
    }

    override fun name() = "tcp-service-discovery"
}
