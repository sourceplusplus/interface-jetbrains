package spp.jetbrains.monitor.skywalking.bridge

import com.apollographql.apollo3.api.Optional
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.protocol.artifact.exception.LiveStackTrace
import spp.protocol.artifact.log.Log
import spp.protocol.artifact.log.LogOrderType
import spp.protocol.artifact.log.LogResult
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
import spp.protocol.util.LocalMessageCodec

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
        val orderType: LogOrderType,
        val pageNumber: Int = 1,
        val pageSize: Int = 10
    )
}
