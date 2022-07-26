/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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
package spp.jetbrains.monitor.skywalking.bridge

import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.protocol.marshall.LocalMessageCodec

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class EndpointBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        val isRegisteredMap = vertx.sharedData().getLocalMap<String, Boolean>("registered_codecs")
        if (!isRegisteredMap.getOrDefault("EndpointBridge", false)) {
            vertx.eventBus().registerDefaultCodec(EndpointQuery::class.java, LocalMessageCodec())
            isRegisteredMap["EndpointBridge"] = true
        }

        vertx.eventBus().localConsumer<String>(searchExactEndpointAddress) {
            launch(vertx.dispatcher()) {
                val service = try {
                    ServiceBridge.getCurrentService(vertx)
                } catch (ex: ReplyException) {
                    if (ex.failureType() == ReplyFailure.TIMEOUT) {
                        log.debug("Timed out looking for current service")
                        it.reply(null)
                        return@launch
                    } else {
                        ex.printStackTrace()
                        it.fail(500, ex.message)
                        return@launch
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    it.fail(404, "Apache SkyWalking current service unavailable")
                    return@launch
                }

                val endpointName = it.body()
                val endpoints = skywalkingClient.searchEndpoint(endpointName, service.id, 10, true)
                if (endpoints.size() != 0) {
                    val exactEndpoint = endpoints.map { it as JsonObject }.find { it.getString("name") == endpointName }
                    if (exactEndpoint != null) {
                        it.reply(exactEndpoint)
                    } else {
                        it.reply(null)
                    }
                } else {
                    it.reply(null)
                }
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
     */
    companion object {
        private val log = LoggerFactory.getLogger(EndpointBridge::class.java)
        private const val rootAddress = "monitor.skywalking.endpoint"
        private const val searchExactEndpointAddress = "$rootAddress.searchExactEndpoint"

        suspend fun searchExactEndpoint(keyword: String, vertx: Vertx): JsonObject? {
            return vertx.eventBus()
                .request<JsonObject?>(searchExactEndpointAddress, keyword)
                .await().body()
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
     */
    data class EndpointQuery(
        val serviceId: String? = null,
        val limit: Int
    )
}
