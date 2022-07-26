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
package spp.jetbrains.monitor.skywalking

import kotlinx.datetime.Instant
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import monitor.skywalking.protocol.trace.QueryBasicTracesQuery
import monitor.skywalking.protocol.trace.QueryTraceQuery
import monitor.skywalking.protocol.type.QueryOrder
import monitor.skywalking.protocol.type.TraceState
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.metrics.ArtifactMetricResult
import spp.protocol.artifact.metrics.ArtifactMetrics
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.artifact.trace.*
import spp.protocol.platform.general.Service

fun toProtocol(
    artifactQualifiedName: ArtifactQualifiedName,
    timeFrame: QueryTimeFrame,
    focus: MetricType,
    metricsRequest: GetEndpointMetrics,
    metrics: List<GetLinearIntValuesQuery.Result>
): ArtifactMetricResult {
    return ArtifactMetricResult(
        artifactQualifiedName = artifactQualifiedName,
        timeFrame = timeFrame,
        focus = focus,
        start = Instant.fromEpochMilliseconds(metricsRequest.zonedDuration.start.toInstant().toEpochMilli()),
        stop = Instant.fromEpochMilliseconds(metricsRequest.zonedDuration.stop.toInstant().toEpochMilli()),
        step = metricsRequest.zonedDuration.step.name,
        artifactMetrics = metrics.mapIndexed { i, result -> result.toProtocol(metricsRequest.metricIds[i]) }
    )
}

fun GetLinearIntValuesQuery.Result.toProtocol(metricType: String): ArtifactMetrics {
    return ArtifactMetrics(
        metricType = MetricType.realValueOf(metricType),
        values = values.map { (it.value as Int).toDouble() }
    )
}

fun QueryBasicTracesQuery.Trace.toProtocol(): Trace {
    return Trace(
        segmentId = segmentId,
        operationNames = endpointNames,
        duration = duration,
        start = Instant.fromEpochMilliseconds(start.toLong()),
        error = isError,
        traceIds = traceIds
    )
}

//todo: correctly
fun QueryTraceQuery.Log.toProtocol(): TraceSpanLogEntry {
    if (data!!.find { it.key == "stack" } != null) {
        return TraceSpanLogEntry(
            time = Instant.fromEpochMilliseconds(time as Long),
            data = data.find { it.key == "stack" }!!.value!! //todo: correctly
        )
    }
    return TraceSpanLogEntry(
        time = Instant.fromEpochMilliseconds(time as Long),
        data = data.joinToString(separator = "\n") { it.key + " : " + it.value!! }
    )
}

fun QueryTraceQuery.Ref.toProtocol(): TraceSpanRef {
    return TraceSpanRef(
        traceId = traceId,
        parentSegmentId = parentSegmentId,
        parentSpanId = parentSpanId,
        type = type.name
    )
}

fun QueryTraceQuery.Span.toProtocol(): TraceSpan {
    return TraceSpan(
        traceId = traceId,
        segmentId = segmentId,
        spanId = spanId,
        parentSpanId = parentSpanId,
        refs = refs.map { it.toProtocol() },
        serviceCode = serviceCode,
        //serviceInstanceName = serviceInstanceName, //todo: this
        startTime = Instant.fromEpochMilliseconds(startTime as Long),
        endTime = Instant.fromEpochMilliseconds(endTime as Long),
        endpointName = endpointName,
        type = type,
        peer = peer,
        component = component,
        error = isError,
        layer = layer,
        tags = tags.associate { it.key to it.value!! },
        logs = logs.map { it.toProtocol() }
    )
}

fun TraceOrderType.toQueryOrder(): QueryOrder {
    return when (this) {
        TraceOrderType.SLOWEST_TRACES -> QueryOrder.BY_DURATION
        else -> QueryOrder.BY_START_TIME
    }
}

fun TraceOrderType.toTraceState(): TraceState {
    return when (this) {
        TraceOrderType.FAILED_TRACES -> TraceState.ERROR
        else -> TraceState.ALL
    }
}

fun Iterable<GetLinearIntValuesQuery.Value>.average(): Double {
    return map { it.value as Int }.average()
}

fun GetMultipleLinearIntValuesQuery.Value.toProtocol(): Double {
    return (value as Int).toDouble()
}

fun GetAllServicesQuery.Result.toProtocol(): Service {
    return Service(id, name)
}
