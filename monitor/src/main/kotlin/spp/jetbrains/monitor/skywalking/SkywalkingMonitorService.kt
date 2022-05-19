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

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.AsyncResult
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import monitor.skywalking.protocol.metadata.SearchEndpointQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import spp.jetbrains.monitor.skywalking.bridge.LogsBridge
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.model.GetMultipleEndpointMetrics
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpanStackQueryResult
import spp.protocol.platform.general.Service

abstract class SkywalkingMonitorService {
    companion object {
        val KEY = Key.create<SkywalkingMonitorService>("SPP_SKYWALKING_MONITOR_SERVICE")

        fun getInstance(project: Project): SkywalkingMonitorService {
            return project.getUserData(KEY)!!
        }
    }

    abstract suspend fun getVersion(): String
    abstract suspend fun getTimeInfo(): GetTimeInfoQuery.Data
    abstract suspend fun searchExactEndpoint(keyword: String): SearchEndpointQuery.Result?
    abstract suspend fun getEndpoints(serviceId: String? = null, limit: Int): List<SearchEndpointQuery.Result>
    abstract suspend fun getMetrics(request: GetEndpointMetrics): List<GetLinearIntValuesQuery.Result>
    abstract suspend fun getMultipleMetrics(request: GetMultipleEndpointMetrics): List<GetMultipleLinearIntValuesQuery.Result>
    abstract suspend fun getTraces(request: GetEndpointTraces): TraceResult
    abstract suspend fun getTraceStack(traceId: String): TraceSpanStackQueryResult
    abstract suspend fun queryLogs(query: LogsBridge.GetEndpointLogs): AsyncResult<LogResult>
    abstract suspend fun getCurrentService(): Service
    abstract suspend fun getActiveServices(): List<Service>
    abstract suspend fun getCurrentServiceInstance(): GetServiceInstancesQuery.Result?
    abstract suspend fun getActiveServiceInstances(): List<GetServiceInstancesQuery.Result>
    abstract suspend fun getServiceInstances(serviceId: String): List<GetServiceInstancesQuery.Result>
}
