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
package spp.jetbrains.monitor.skywalking.impl

import io.vertx.core.AsyncResult
import io.vertx.core.json.JsonArray
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import monitor.skywalking.protocol.metadata.SearchEndpointQuery
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
) : SkywalkingMonitorService() {

    override suspend fun getVersion(): String {
        return skywalkingClient.getVersion()!!
    }

    override suspend fun getTimeInfo(): GetTimeInfoQuery.Data {
        return skywalkingClient.getTimeInfo()
    }

    override suspend fun queryLogs(query: LogsBridge.GetEndpointLogs): AsyncResult<LogResult> {
        return LogsBridge.queryLogs(query, skywalkingClient.vertx)
    }

    override suspend fun searchExactEndpoint(keyword: String): SearchEndpointQuery.Result? {
        return EndpointBridge.searchExactEndpoint(keyword, skywalkingClient.vertx)
    }

    override suspend fun getEndpoints(serviceId: String?, limit: Int): List<SearchEndpointQuery.Result> {
        return EndpointBridge.getEndpoints(serviceId, limit, skywalkingClient.vertx)
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

    override suspend fun getCurrentService(): Service {
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

    override suspend fun sortMetrics(condition: TopNCondition, duration: ZonedDuration): JsonArray {
        return skywalkingClient.sortMetrics(condition, duration)
    }
}
