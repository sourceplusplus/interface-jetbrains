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
package spp.jetbrains.monitor.skywalking

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.codahale.metrics.MetricRegistry
import io.vertx.core.Vertx
import monitor.skywalking.protocol.log.QueryLogsQuery
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import monitor.skywalking.protocol.metadata.SearchEndpointQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import monitor.skywalking.protocol.trace.QueryBasicTracesQuery
import monitor.skywalking.protocol.trace.QueryTraceQuery
import monitor.skywalking.protocol.type.*
import org.slf4j.LoggerFactory
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.model.GetMultipleEndpointMetrics
import spp.protocol.util.LocalMessageCodec
import java.io.IOException
import java.time.ZoneOffset.ofHours
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Used to communicate with Apache SkyWalking.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SkywalkingClient(
    val vertx: Vertx,
    private val apolloClient: ApolloClient,
    private val timezoneOffset: Int = 0
) {

    companion object {
        private val metricRegistry: MetricRegistry = MetricRegistry()
        private val log = LoggerFactory.getLogger(SkywalkingClient::class.java)

        fun registerCodecs(vertx: Vertx) {
            log.info("Registering Apache SkyWalking codecs")
            val isRegisteredMap = vertx.sharedData().getLocalMap<String, Boolean>("registered_codecs")
            if (!isRegisteredMap.getOrDefault("SkywalkingClient", false)) {
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
                isRegisteredMap["SkywalkingClient"] = true
            }
        }
    }

    init {
        registerCodecs(vertx)
    }

    suspend fun queryTraceStack(
        traceId: String,
    ): QueryTraceQuery.Result? {
        metricRegistry.timer("queryTraceStack").time().use {
            if (log.isTraceEnabled) log.trace("Query trace stack request. Trace: {}", traceId)

            val response = apolloClient.query(QueryTraceQuery(traceId)).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.error(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Query trace stack response: {}", response.data!!.result)
                return response.data!!.result
            }
        }
    }

    suspend fun queryBasicTraces(request: GetEndpointTraces): QueryBasicTracesQuery.Result? {
        metricRegistry.timer("queryBasicTraces").time().use {
            if (log.isTraceEnabled) log.trace("Query basic traces request. Request: {}", request)

            val response = apolloClient.query(
                QueryBasicTracesQuery(
                    TraceQueryCondition(
                        serviceId = Optional.presentIfNotNull(request.serviceId),
                        serviceInstanceId = Optional.presentIfNotNull(request.serviceInstanceId),
                        endpointId = Optional.presentIfNotNull(request.endpointId),
                        endpointName = Optional.presentIfNotNull(request.endpointName),
                        queryDuration = Optional.Present(request.zonedDuration.toDuration(this)),
                        queryOrder = request.orderType.toQueryOrder(),
                        traceState = request.orderType.toTraceState(),
                        paging = Pagination(Optional.Present(request.pageNumber), request.pageSize)
                    )
                )
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.error(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Query basic traces response: {}", response.data!!.result)
                return response.data!!.result
            }
        }
    }

    suspend fun getEndpointMetrics(
        metricName: String,
        endpointId: String,
        duration: Duration
    ): GetLinearIntValuesQuery.Result? {
        metricRegistry.timer("getEndpointMetrics").time().use {
            if (log.isTraceEnabled) {
                log.trace(
                    "Get endpoint metrics request. Metric: {} - Endpoint: {} - Duration: {}",
                    metricName, endpointId, duration
                )
            }

            val response = apolloClient.query(
                GetLinearIntValuesQuery(MetricCondition(metricName, Optional.Present(endpointId)), duration)
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.error(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Get endpoint metrics response: {}", response.data!!.result)
                return response.data!!.result
            }
        }
    }

    suspend fun getMultipleEndpointMetrics(
        metricName: String,
        endpointId: String,
        numOfLinear: Int,
        duration: Duration
    ): List<GetMultipleLinearIntValuesQuery.Result> {
        metricRegistry.timer("getMultipleEndpointMetrics").time().use {
            if (log.isTraceEnabled) {
                log.trace(
                    "Get multiple endpoint metrics request. Metric: {} - Endpoint: {} - Number: {} - Duration: {}",
                    metricName, endpointId, numOfLinear, duration
                )
            }

            val response = apolloClient.query(
                GetMultipleLinearIntValuesQuery(
                    MetricCondition(metricName, Optional.Present(endpointId)),
                    numOfLinear,
                    duration
                )
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.error(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Get multiple endpoint metrics response: {}", response.data!!.result)
                return response.data!!.result
            }
        }
    }

    suspend fun searchEndpoint(keyword: String, serviceId: String, limit: Int): List<SearchEndpointQuery.Result> {
        metricRegistry.timer("searchEndpoint").time().use {
            if (log.isTraceEnabled) {
                log.trace(
                    "Search endpoint request. Keyword: {} - Service: {} - Limit: {}", keyword, serviceId, limit
                )
            }

            val response = apolloClient.query(
                SearchEndpointQuery(keyword, serviceId, limit)
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.error(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Search endpoint response: {}", response.data!!.result)
                return response.data!!.result
            }
        }
    }

    suspend fun queryLogs(condition: LogQueryCondition): QueryLogsQuery.Result? {
        metricRegistry.timer("queryLogs").time().use {
            if (log.isTraceEnabled) log.trace("Query logs request. Condition: {}", condition)

            val response = apolloClient.query(
                QueryLogsQuery(Optional.Present(condition))
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.error(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Query logs response: {}", response.data!!.result)
                return response.data!!.result //todo: change return type if this can never be null
            }
        }
    }

    suspend fun getServices(duration: Duration): List<GetAllServicesQuery.Result> {
        metricRegistry.timer("getServices").time().use {
            if (log.isTraceEnabled) log.trace("Get services request. Duration: {}", duration)

            val response = apolloClient.query(
                GetAllServicesQuery(duration)
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.error(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Get services response: {}", response.data!!.result)
                return response.data!!.result
            }
        }
    }

    suspend fun getServiceInstances(serviceId: String, duration: Duration): List<GetServiceInstancesQuery.Result> {
        metricRegistry.timer("getServiceInstances").time().use {
            if (log.isTraceEnabled) {
                log.trace("Get service instances request. Service: {} - Duration: {}", serviceId, duration)
            }

            val response = apolloClient.query(
                GetServiceInstancesQuery(serviceId, duration)
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.error(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Get service instances: {}", response.data!!.result)
                return response.data!!.result
            }
        }
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
}
