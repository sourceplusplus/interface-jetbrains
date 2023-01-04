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
package spp.jetbrains.monitor.skywalking.bridge

import com.apollographql.apollo3.api.Optional
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.type.LogQueryCondition
import monitor.skywalking.protocol.type.Pagination
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.jetbrains.monitor.skywalking.toDuration
import spp.protocol.artifact.exception.LiveStackTrace
import spp.protocol.artifact.log.Log
import spp.protocol.artifact.log.LogOrderType
import spp.protocol.artifact.log.LogResult
import spp.protocol.marshall.LocalMessageCodec
import java.time.Instant

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class LogsBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        val isRegisteredMap = vertx.sharedData().getLocalMap<String, Boolean>("registered_codecs")
        if (!isRegisteredMap.getOrDefault("LogsBridge", false)) {
            vertx.eventBus().registerDefaultCodec(GetEndpointLogs::class.java, LocalMessageCodec())
            isRegisteredMap["LogsBridge"] = true
        }

        vertx.eventBus().localConsumer<GetEndpointLogs>(queryEndpointLogsAddress) {
            launch(vertx.dispatcher()) {
                val request = it.body()
                val logs = skywalkingClient.queryLogs(
                    LogQueryCondition(
                        serviceId = Optional.presentIfNotNull(request.serviceId),
                        queryDuration = Optional.Present(request.zonedDuration.toDuration(skywalkingClient)),
                        paging = Pagination(Optional.Present(request.pageNumber), request.pageSize)
                    )
                )
                if (logs != null) {
                    it.reply(
                        LogResult(
                            orderType = request.orderType,
                            timestamp = Instant.now(),
                            logs = logs.logs.map {
                                val exceptionStr = it.tags?.find { it.key == "exception" }?.value
                                val epoch = if (it.timestamp is String) {
                                    it.timestamp.toString().toLong()
                                } else {
                                    it.timestamp as Long
                                }
                                Log(
                                    Instant.ofEpochMilli(epoch),
                                    it.content.orEmpty(),
                                    level = it.tags!!.find { it.key == "level" }?.value!!,
                                    logger = it.tags.find { it.key == "logger" }?.value,
                                    thread = it.tags.find { it.key == "thread" }?.value,
                                    arguments = it.tags.filter { it.key.startsWith("argument.") }
                                        .mapNotNull { it.value },
                                    exception = if (exceptionStr != null) LiveStackTrace.fromString(exceptionStr) else null
                                )
                            })
                    )
                } else {
                    it.fail(500, "todo")
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
        private const val rootAddress = "monitor.skywalking.endpoint.logs"
        private const val queryEndpointLogsAddress = "$rootAddress.queryEndpointLogs"

        suspend fun queryLogs(query: GetEndpointLogs, vertx: Vertx): AsyncResult<LogResult> {
            return try { //todo: follow this pattern in the other bridge helper methods
                val value = vertx.eventBus()
                    .request<LogResult>(queryEndpointLogsAddress, query)
                    .await()
                Future.succeededFuture(value.body())
            } catch (throwable: Throwable) {
                Future.failedFuture(throwable)
            }
        }
    }

    /**
     * todo: description.
     *
     * @since 0.1.0
     * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
     */
    @Suppress("MagicNumber")
    data class GetEndpointLogs(
        val serviceId: String? = null,
        val endpointId: String? = null,
        val zonedDuration: ZonedDuration,
        val orderType: LogOrderType = LogOrderType.NEWEST_LOGS,
        val pageNumber: Int = 1,
        val pageSize: Int = 10
    )
}
