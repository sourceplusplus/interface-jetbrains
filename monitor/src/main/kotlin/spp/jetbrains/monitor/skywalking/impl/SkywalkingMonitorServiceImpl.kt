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
package spp.jetbrains.monitor.skywalking.impl

import io.vertx.core.AsyncResult
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import monitor.skywalking.protocol.type.TopNCondition
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.SkywalkingMonitorService
import spp.jetbrains.monitor.skywalking.bridge.*
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.model.GetMultipleEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpanStackQueryResult
import spp.protocol.platform.general.Service

class SkywalkingMonitorServiceImpl(
    private val skywalkingClient: SkywalkingClient
) : SkywalkingMonitorService {

    override suspend fun getVersion(): String {
        return skywalkingClient.getVersion()!!
    }

    override suspend fun getTimeInfo(): GetTimeInfoQuery.Data {
        return skywalkingClient.getTimeInfo()
    }

    override suspend fun queryLogs(query: LogsBridge.GetEndpointLogs): AsyncResult<LogResult> {
        return LogsBridge.queryLogs(query, skywalkingClient.vertx)
    }

    override suspend fun searchExactEndpoint(keyword: String, cache: Boolean): JsonObject? {
        val service = getCurrentService() ?: return null
        val endpoints = skywalkingClient.searchEndpoint(keyword, service.id, 1, cache)
        return endpoints.map { it as JsonObject }.find { it.getString("name") == keyword }
    }

    override suspend fun getEndpoints(serviceId: String, limit: Int, cache: Boolean): JsonArray {
        return skywalkingClient.searchEndpoint("", serviceId, limit, cache)
    }

    override suspend fun getMetrics(request: GetEndpointMetrics): List<GetLinearIntValuesQuery.Result> {
        return EndpointMetricsBridge.getMetrics(request, skywalkingClient.vertx)
    }

    override suspend fun getMultipleMetrics(request: GetMultipleEndpointMetrics): List<GetMultipleLinearIntValuesQuery.Result> {
        return EndpointMetricsBridge.getMultipleMetrics(request, skywalkingClient.vertx)
    }

    override suspend fun getTraces(request: GetEndpointTraces): TraceResult {
        return EndpointTracesBridge.getTraces(request, skywalkingClient.vertx)
    }

    override suspend fun getTraceStack(traceId: String): TraceSpanStackQueryResult {
        return EndpointTracesBridge.getTraceStack(traceId, skywalkingClient.vertx)
    }

    override suspend fun getCurrentService(): Service? {
        return ServiceBridge.getCurrentService(skywalkingClient.vertx)
    }

    override suspend fun getActiveServices(): List<Service> {
        return ServiceBridge.getActiveServices(skywalkingClient.vertx)
    }

    override suspend fun getCurrentServiceInstance(): GetServiceInstancesQuery.Result? {
        return ServiceInstanceBridge.getCurrentServiceInstance(skywalkingClient.vertx)
    }

    override suspend fun getActiveServiceInstances(): List<GetServiceInstancesQuery.Result> {
        return ServiceInstanceBridge.getActiveServiceInstances(skywalkingClient.vertx)
    }

    override suspend fun getServiceInstances(serviceId: String): List<GetServiceInstancesQuery.Result> {
        return ServiceInstanceBridge.getServiceInstances(serviceId, skywalkingClient.vertx)
    }

    override suspend fun sortMetrics(condition: TopNCondition, duration: ZonedDuration, cache: Boolean): JsonArray {
        return skywalkingClient.sortMetrics(condition, duration, cache)
    }
}
