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
package spp.jetbrains.monitor.skywalking

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.codahale.metrics.MetricRegistry
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.diagnostic.logger
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import monitor.skywalking.protocol.log.QueryLogsQuery
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import monitor.skywalking.protocol.metadata.SearchEndpointQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.SortMetricsQuery
import monitor.skywalking.protocol.trace.QueryBasicTracesQuery
import monitor.skywalking.protocol.type.*
import spp.jetbrains.monitor.skywalking.model.*
import spp.jetbrains.monitor.skywalking.model.TopNCondition
import spp.protocol.marshall.LocalMessageCodec
import spp.protocol.platform.general.Service
import java.io.IOException
import java.time.ZoneOffset.ofHours
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

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
        private val log = logger<SkywalkingClient>()

        fun registerCodecs(vertx: Vertx) {
            log.info("Registering Apache SkyWalking codecs")
            val isRegisteredMap = vertx.sharedData().getLocalMap<String, Boolean>("registered_codecs")
            if (!isRegisteredMap.getOrDefault("SkywalkingClient", false)) {
                vertx.eventBus().registerDefaultCodec(GetMultipleEndpointMetrics::class.java, LocalMessageCodec())
                vertx.eventBus().registerDefaultCodec(GetEndpointTraces::class.java, LocalMessageCodec())
                vertx.eventBus().registerDefaultCodec(GetEndpointMetrics::class.java, LocalMessageCodec())
                vertx.eventBus().registerDefaultCodec(Service::class.java, LocalMessageCodec())
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

    private val oneMinRespCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<Any, Any>()

    init {
        registerCodecs(vertx)
    }

    suspend fun queryBasicTraces(request: GetEndpointTraces): QueryBasicTracesQuery.Result? {
        metricRegistry.timer("queryBasicTraces").time().use {
            if (log.isTraceEnabled) log.trace("Query basic traces request. Request: $request")

            val response = apolloClient.query(
                QueryBasicTracesQuery(
                    TraceQueryCondition(
                        serviceId = Optional.presentIfNotNull(request.serviceId),
                        serviceInstanceId = Optional.presentIfNotNull(request.serviceInstanceId),
                        endpointId = Optional.presentIfNotNull(request.endpointId),
                        queryDuration = Optional.Present(request.zonedDuration.toDuration(this)),
                        queryOrder = request.orderType.toQueryOrder(),
                        traceState = request.orderType.toTraceState(),
                        paging = Pagination(Optional.Present(request.pageNumber), request.pageSize)
                    )
                )
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.warn(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Query basic traces response: ${response.data!!.result}")
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
                    "Get endpoint metrics request. Metric: $metricName - Endpoint: $endpointId - Duration: $duration"
                )
            }

            val response = apolloClient.query(
                GetLinearIntValuesQuery(MetricCondition(metricName, Optional.Present(endpointId)), duration)
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.warn(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Get endpoint metrics response: ${response.data!!.result}")
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
                    "Get multiple endpoint metrics request. Metric: $metricName - Endpoint: $endpointId - Number: $numOfLinear - Duration: $duration"
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
                response.errors!!.forEach { log.warn(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Get multiple endpoint metrics response: ${response.data!!.result}")
                return response.data!!.result
            }
        }
    }

    suspend fun searchEndpoint(keyword: String, serviceId: String, limit: Int, cache: Boolean): JsonArray {
        if (cache) {
            oneMinRespCache.getIfPresent(Triple(keyword, serviceId, limit))?.let {
                return it as JsonArray
            }
        }

        metricRegistry.timer("searchEndpoint").time().use {
            if (log.isTraceEnabled) {
                log.trace("Search endpoint request. Keyword: $keyword - Service: $serviceId - Limit: $limit")
            }

            val response = apolloClient.query(
                SearchEndpointQuery(keyword, serviceId, limit)
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.warn(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Search endpoint response: ${response.data!!.result}")
                val resp = JsonArray(Json.encode(response.data!!.result))
                oneMinRespCache.put(Triple(keyword, serviceId, limit), resp)
                return resp
            }
        }
    }

    suspend fun queryLogs(condition: LogQueryCondition): QueryLogsQuery.Result? {
        metricRegistry.timer("queryLogs").time().use {
            if (log.isTraceEnabled) log.trace("Query logs request. Condition: $condition")

            val response = apolloClient.query(
                QueryLogsQuery(Optional.Present(condition))
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.warn(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Query logs response: ${response.data!!.result}")
                return response.data!!.result //todo: change return type if this can never be null
            }
        }
    }

    suspend fun getServices(duration: Duration): List<Service> {
        metricRegistry.timer("getServices").time().use {
            if (log.isTraceEnabled) log.trace("Get services request. Duration: $duration")

            val response = apolloClient.query(
                GetAllServicesQuery(duration)
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.warn(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Get services response: ${response.data!!.result}")
                return response.data!!.result.map { it.toProtocol() }
            }
        }
    }

    suspend fun getServiceInstances(serviceId: String, duration: Duration): List<GetServiceInstancesQuery.Result> {
        metricRegistry.timer("getServiceInstances").time().use {
            if (log.isTraceEnabled) {
                log.trace("Get service instances request. Service: $serviceId - Duration: $duration")
            }

            val response = apolloClient.query(
                GetServiceInstancesQuery(serviceId, duration)
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.warn(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Get service instances: $response.data!!.result")
                return response.data!!.result
            }
        }
    }

    suspend fun sortMetrics(condition: TopNCondition, duration: ZonedDuration, cache: Boolean): JsonArray {
        if (cache) {
            oneMinRespCache.getIfPresent(Pair(condition, duration))?.let {
                return it as JsonArray
            }
        }

        metricRegistry.timer("sortMetrics").time().use {
            if (log.isTraceEnabled) {
                log.trace("Sort metrics request. Condition: $condition - Duration: $duration")
            }

            val response = apolloClient.query(
                SortMetricsQuery(condition.fromProtocol(), duration.toDuration(this))
            ).execute()
            if (response.hasErrors()) {
                response.errors!!.forEach { log.warn(it.message) }
                throw IOException(response.errors!![0].message)
            } else {
                if (log.isTraceEnabled) log.trace("Sort metrics response: ${response.data!!.result}")
                val resp = JsonArray(Json.encode(response.data!!.result))
                oneMinRespCache.put(Pair(condition, duration), resp)
                return resp
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
}
