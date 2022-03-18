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

import com.apollographql.apollo3.api.Optional
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import monitor.skywalking.protocol.type.LogQueryCondition
import monitor.skywalking.protocol.type.Pagination
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.protocol.artifact.exception.LiveStackTrace
import spp.protocol.artifact.log.Log
import spp.protocol.artifact.log.LogOrderType
import spp.protocol.artifact.log.LogResult
import spp.protocol.marshall.LocalMessageCodec

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
                            timestamp = Clock.System.now(),
                            logs = logs.logs.map {
                                val exceptionStr = it.tags?.find { it.key == "exception" }?.value
                                val epoch = if (it.timestamp is String) {
                                    it.timestamp.toString().toLong()
                                } else {
                                    it.timestamp as Long
                                }
                                Log(
                                    Instant.fromEpochMilliseconds(epoch),
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
