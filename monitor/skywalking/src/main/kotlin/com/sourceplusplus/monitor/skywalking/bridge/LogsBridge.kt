package com.sourceplusplus.monitor.skywalking.bridge

import com.apollographql.apollo.api.Input
import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.SkywalkingClient.LocalMessageCodec
import com.sourceplusplus.monitor.skywalking.model.ZonedDuration
import com.sourceplusplus.protocol.artifact.log.Log
import com.sourceplusplus.protocol.artifact.log.LogOrderType
import com.sourceplusplus.protocol.artifact.log.LogResult
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import monitor.skywalking.protocol.type.LogQueryCondition
import monitor.skywalking.protocol.type.LogState
import monitor.skywalking.protocol.type.Pagination

/**
 * todo: description.
 *
 * @since 0.1.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LogsBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    override suspend fun start() {
        val isRegisteredMap = vertx.sharedData().getLocalMap<String, Boolean>("registered_codecs")
        if (!isRegisteredMap.getOrDefault("LogsBridge", false)) {
            vertx.eventBus().registerDefaultCodec(GetEndpointLogs::class.java, LocalMessageCodec())
            isRegisteredMap["LogsBridge"] = true
        }

        vertx.eventBus().localConsumer<GetEndpointLogs>(queryEndpointLogsAddress) {
            launch(vertx.dispatcher()) {
                val service = ServiceBridge.getCurrentService(vertx)
                if (service != null) {
                    val request = it.body()
                    val logs = skywalkingClient.queryLogs(
                        LogQueryCondition(
                            //endpointId = Input.optional(request.endpointId),
                            queryDuration = Input.optional(request.zonedDuration.toDuration(skywalkingClient)),
                            state = LogState.ALL,
                            paging = Pagination(pageSize = 10)
                        )
                    )
                    if (logs != null) {
                        it.reply(
                            LogResult(
                                orderType = request.orderType,
                                timestamp = Clock.System.now(),
                                logs = logs.logs.map {
                                    Log(
                                        Instant.fromEpochMilliseconds(it.timestamp.toLong()),
                                        it.content.orEmpty()
                                    )
                                })
                        )
                    } else {
                        it.fail(500, "todo")
                    }
                } else {
                    it.fail(500, "todo")
                }
            }
        }
    }

    companion object {
        private const val rootAddress = "monitor.skywalking.endpoint.logs"
        private const val queryEndpointLogsAddress = "$rootAddress.queryEndpointLogs"

        suspend fun queryLogs(query: GetEndpointLogs, vertx: Vertx): LogResult {
            return vertx.eventBus()
                .request<LogResult>(queryEndpointLogsAddress, query)
                .await().body()
        }
    }

    data class GetEndpointLogs(
        val endpointId: String,
        val zonedDuration: ZonedDuration,
        val orderType: LogOrderType
    )
}
