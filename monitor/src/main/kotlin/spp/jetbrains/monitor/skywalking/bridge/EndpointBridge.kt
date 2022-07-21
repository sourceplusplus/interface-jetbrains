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
