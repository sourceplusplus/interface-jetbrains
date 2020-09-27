package com.sourceplusplus.monitor.skywalking

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.toDeferred
import com.sourceplusplus.monitor.skywalking.model.GetEndpointMetrics
import com.sourceplusplus.monitor.skywalking.model.GetEndpointTraces
import com.sourceplusplus.monitor.skywalking.model.GetMultipleEndpointMetrics
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import monitor.skywalking.protocol.metadata.SearchEndpointQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import monitor.skywalking.protocol.trace.QueryBasicTracesQuery
import monitor.skywalking.protocol.trace.QueryTraceQuery
import monitor.skywalking.protocol.type.*
import org.slf4j.LoggerFactory
import java.time.ZoneOffset.ofHours
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingClient(
    val vertx: Vertx,
    private val apolloClient: ApolloClient,
    private val timezoneOffset: Int = 0
) {

    companion object {
        private val log = LoggerFactory.getLogger(SkywalkingClient::class.java)

        fun registerCodecs(vertx: Vertx) {
            log.info("Registering Apache SkyWalking codecs")
            vertx.eventBus().registerDefaultCodec(GetMultipleEndpointMetrics::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(GetEndpointTraces::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(GetEndpointMetrics::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(GetAllServicesQuery.Result::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(GetServiceInstancesQuery.Result::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(SearchEndpointQuery.Result::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(QueryBasicTracesQuery.Result::class.java, LocalMessageCodec())
            vertx.eventBus().registerDefaultCodec(
                ArrayList::class.java, LocalMessageCodec()
            ) //todo: should likely wrap in object
        }
    }

    init {
        registerCodecs(vertx)
    }

    suspend fun queryTraceStack(
        traceId: String,
    ): QueryTraceQuery.Result? {
        val response = apolloClient.query(QueryTraceQuery(traceId)).toDeferred().await()

        //todo: throw error if failed
        return response.data!!.result
    }

    suspend fun queryBasicTraces(request: GetEndpointTraces): QueryBasicTracesQuery.Result? {
        val response = apolloClient.query(
            QueryBasicTracesQuery(
                TraceQueryCondition(
                    endpointId = Input.optional(request.endpointId),
                    queryDuration = Input.optional(request.zonedDuration.toDuration(this)),
                    queryOrder = request.orderType.toQueryOrder(),
                    traceState = request.orderType.toTraceState(),
                    paging = Pagination(Input.optional(request.pageNumber), request.pageSize)
                )
            )
        ).toDeferred().await()

        //todo: throw error if failed
        return response.data!!.result
    }

    suspend fun getEndpointMetrics(
        metricName: String,
        endpointId: String,
        duration: Duration
    ): GetLinearIntValuesQuery.Result? {
        val response = apolloClient.query(
            GetLinearIntValuesQuery(MetricCondition(metricName, Input.optional(endpointId)), duration)
        ).toDeferred().await()

        //todo: throw error if failed
        return response.data!!.result
    }

    suspend fun getMultipleEndpointMetrics(
        metricName: String,
        endpointId: String,
        numOfLinear: Int,
        duration: Duration
    ): List<GetMultipleLinearIntValuesQuery.Result> {
        val response = apolloClient.query(
            GetMultipleLinearIntValuesQuery(
                MetricCondition(metricName, Input.optional(endpointId)),
                numOfLinear,
                duration
            )
        ).toDeferred().await()

        //todo: throw error if failed
        return response.data!!.result
    }

    suspend fun searchEndpoint(keyword: String, serviceId: String, limit: Int): List<SearchEndpointQuery.Result> {
        val response = apolloClient.query(
            SearchEndpointQuery(keyword, serviceId, limit)
        ).toDeferred().await()

        //todo: throw error if failed
        return response.data!!.result
    }

    suspend fun getServices(duration: Duration): List<GetAllServicesQuery.Result> {
        val response = apolloClient.query(
            GetAllServicesQuery(duration)
        ).toDeferred().await()

        //todo: throw error if failed
        return response.data!!.result
    }

    suspend fun getServiceInstances(serviceId: String, duration: Duration): List<GetServiceInstancesQuery.Result> {
        val response = apolloClient.query(
            GetServiceInstancesQuery(serviceId, duration)
        ).toDeferred().await()

        //todo: throw error if failed
        return response.data!!.result
    }

    fun getDuration(since: ZonedDateTime, step: DurationStep): Duration {
        return getDuration(since, ZonedDateTime.now(), step)
    }

    fun getDuration(from: ZonedDateTime, to: ZonedDateTime, step: DurationStep): Duration {
        val fromDate = from.withZoneSameInstant(ofHours(timezoneOffset))
        val toDate = to.withZoneSameInstant(ofHours(timezoneOffset))
        return Duration(
            fromDate.format(step.dateTimeFormatter),
            toDate.format(step.dateTimeFormatter),
            Step.valueOf(step.name)
        )
    }

    enum class DurationStep(val dateTimeFormatter: DateTimeFormatter) {
        DAY(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
        HOUR(DateTimeFormatter.ofPattern("yyyy-MM-dd HH")),
        MINUTE(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm")),
        SECOND(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss"))
    }

    /**
     * todo: description.
     *
     * @since 0.0.1
     */
    class LocalMessageCodec<T> internal constructor() : MessageCodec<T, T> {
        override fun encodeToWire(buffer: Buffer, o: T): Unit =
            throw UnsupportedOperationException("Not supported yet.")

        override fun decodeFromWire(pos: Int, buffer: Buffer): T =
            throw UnsupportedOperationException("Not supported yet.")

        override fun transform(o: T): T = o
        override fun name(): String = UUID.randomUUID().toString()
        override fun systemCodecID(): Byte = -1
    }
}
