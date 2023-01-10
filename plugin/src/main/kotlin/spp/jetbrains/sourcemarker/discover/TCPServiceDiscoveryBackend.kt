/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.discover

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
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
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.spi.ServiceDiscoveryBackend
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.config.SourceMarkerConfig
import spp.jetbrains.sourcemarker.config.getServicePortNormalized
import spp.jetbrains.sourcemarker.config.isSsl
import spp.jetbrains.sourcemarker.config.serviceHostNormalized
import spp.jetbrains.status.SourceStatus.ConnectionError
import spp.jetbrains.status.SourceStatusService
import spp.protocol.platform.PlatformAddress
import spp.protocol.platform.status.InstanceConnection
import spp.protocol.service.SourceServices.LIVE_INSTRUMENT
import spp.protocol.service.SourceServices.LIVE_MANAGEMENT
import spp.protocol.service.SourceServices.LIVE_VIEW
import spp.protocol.service.extend.TCPServiceFrameParser
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
        private val log = logger<TCPServiceDiscoveryBackend>()
        private val projectMap = mutableMapOf<String, TCPServiceDiscoveryBackend>()

        //todo: add register function and remove this
        fun getSocket(project: Project): NetSocket? {
            return projectMap[project.locationHash]?.socket
        }

        suspend fun closeSocket(project: Project) {
            projectMap[project.locationHash]?.socket?.close()?.await()
            projectMap[project.locationHash]?.socket = null
        }
    }

    var socket: NetSocket? = null
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
        projectMap[config.getString("project_location_hash")] = this

        val serviceHost = pluginConfig.serviceHostNormalized
        val certificatePins = mutableListOf<String>()
        certificatePins.addAll(pluginConfig.certificatePins)

        vertx.safeLaunch {
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
                socket = client.connect(pluginConfig.getServicePortNormalized(), serviceHost).await()
            } catch (ex: Exception) {
                log.warn("Failed to connect to service discovery server", ex)
                setupPromise.fail(ex)
                return@safeLaunch
            }
            socket!!.handler(FrameParser(TCPServiceFrameParser(vertx, socket!!)))
            socket!!.exceptionHandler {
                log.warn("Service discovery socket exception", it)
                val project = ProjectManager.getInstance().openProjects.find {
                    it.locationHash == config.getString("project_location_hash")
                }
                if (project != null) {
                    SourceStatusService.getInstance(project).update(ConnectionError, it.message)
                } else {
                    log.warn("Unable to find project. Failed to report status update")
                }
            }
            socket!!.closeHandler {
                log.warn("Service discovery socket closed")
                val project = ProjectManager.getInstance().openProjects.find {
                    it.locationHash == config.getString("project_location_hash")
                }
                if (project != null) {
                    SourceStatusService.getInstance(project).update(ConnectionError, "Service discovery socket closed")
                } else {
                    log.warn("Unable to find project. Failed to report status update")
                }
            }

            vertx.executeBlocking<Any> {
                setupHandler(vertx, "get-records")
                setupHandler(vertx, LIVE_MANAGEMENT)
                setupHandler(vertx, LIVE_INSTRUMENT)
                setupHandler(vertx, LIVE_VIEW)

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
                    BridgeEventType.SEND.name.lowercase(),
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
                BridgeEventType.SEND.name.lowercase(),
                address, replyAddress, headers, true, resp.body(), socket!!
            )
        }
    }

    override fun store(record: Record, resultHandler: Handler<AsyncResult<Record>>) = Unit
    override fun remove(record: Record, resultHandler: Handler<AsyncResult<Record>>) = Unit
    override fun remove(uuid: String, resultHandler: Handler<AsyncResult<Record>>) = Unit
    override fun update(record: Record, resultHandler: Handler<AsyncResult<Void>>) = Unit

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

    override fun getRecord(uuid: String, resultHandler: Handler<AsyncResult<Record>>) = Unit
    override fun name() = "tcp-service-discovery"
}
