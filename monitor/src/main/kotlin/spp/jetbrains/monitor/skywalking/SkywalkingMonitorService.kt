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
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import monitor.skywalking.protocol.metadata.GetTimeInfoQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import monitor.skywalking.protocol.type.TopNCondition
import spp.jetbrains.monitor.skywalking.bridge.LogsBridge
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.model.GetMultipleEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpanStackQueryResult
import spp.protocol.platform.general.Service

interface SkywalkingMonitorService {
    companion object {
        val KEY = Key.create<SkywalkingMonitorService>("SPP_SKYWALKING_MONITOR_SERVICE")

        fun getInstance(project: Project): SkywalkingMonitorService {
            return project.getUserData(KEY)!!
        }
    }

    suspend fun getVersion(): String
    suspend fun getTimeInfo(): GetTimeInfoQuery.Data
    suspend fun searchExactEndpoint(keyword: String, cache: Boolean = false): JsonObject?
    suspend fun getEndpoints(
        serviceId: String,
        limit: Int,
        cache: Boolean = true
    ): JsonArray

    suspend fun getMetrics(request: GetEndpointMetrics): List<GetLinearIntValuesQuery.Result>
    suspend fun getMultipleMetrics(request: GetMultipleEndpointMetrics): List<GetMultipleLinearIntValuesQuery.Result>
    suspend fun getTraces(request: GetEndpointTraces): TraceResult
    suspend fun getTraceStack(traceId: String): TraceSpanStackQueryResult
    suspend fun queryLogs(query: LogsBridge.GetEndpointLogs): AsyncResult<LogResult>
    suspend fun getCurrentService(): Service
    suspend fun getActiveServices(): List<Service>
    suspend fun getCurrentServiceInstance(): GetServiceInstancesQuery.Result?
    suspend fun getActiveServiceInstances(): List<GetServiceInstancesQuery.Result>
    suspend fun getServiceInstances(serviceId: String): List<GetServiceInstancesQuery.Result>
    suspend fun sortMetrics(
        condition: TopNCondition,
        duration: ZonedDuration,
        cache: Boolean = true
    ): JsonArray
}
