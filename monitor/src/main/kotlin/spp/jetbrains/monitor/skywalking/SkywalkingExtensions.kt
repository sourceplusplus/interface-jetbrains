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

import com.apollographql.apollo3.api.Optional
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.trace.QueryBasicTracesQuery
import monitor.skywalking.protocol.trace.QueryTraceQuery
import monitor.skywalking.protocol.type.*
import spp.jetbrains.monitor.skywalking.model.DurationStep
import spp.jetbrains.monitor.skywalking.model.TopNCondition
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.protocol.artifact.trace.*
import spp.protocol.artifact.trace.Trace
import spp.protocol.platform.general.Service
import java.time.Instant

fun QueryBasicTracesQuery.Trace.toProtocol(): Trace {
    return Trace(
        segmentId = segmentId,
        operationNames = endpointNames,
        duration = duration,
        start = Instant.ofEpochMilli(start.toLong()),
        error = isError,
        traceIds = traceIds
    )
}

fun QueryTraceQuery.Log.toProtocol(): TraceSpanLogEntry {
    if (data!!.find { it.key == "stack" } != null) {
        return TraceSpanLogEntry(
            time = Instant.ofEpochMilli(time as Long),
            data = data.find { it.key == "stack" }!!.value!!
        )
    }
    return TraceSpanLogEntry(
        time = Instant.ofEpochMilli(time as Long),
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
        //serviceInstanceName = serviceInstanceName,
        startTime = Instant.ofEpochMilli(startTime as Long),
        endTime = Instant.ofEpochMilli(endTime as Long),
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

fun GetAllServicesQuery.Result.toProtocol(): Service {
    return Service(id, name)
}

fun ZonedDuration.toDuration(skywalkingClient: SkywalkingClient): Duration {
    //minus on stop as skywalking stop is inclusive
    return when (step) {
        DurationStep.SECOND -> skywalkingClient.getDuration(start, stop.minusSeconds(1), step)
        DurationStep.MINUTE -> skywalkingClient.getDuration(start, stop.minusMinutes(1), step)
        DurationStep.HOUR -> skywalkingClient.getDuration(start, stop.minusHours(1), step)
        DurationStep.DAY -> skywalkingClient.getDuration(start, stop.minusDays(1), step)
    }
}

fun TopNCondition.fromProtocol(): monitor.skywalking.protocol.type.TopNCondition {
    return monitor.skywalking.protocol.type.TopNCondition(
        name,
        Optional.presentIfNotNull(parentService),
        Optional.presentIfNotNull(normal),
        Optional.presentIfNotNull(scope.let { if (it == null) null else Scope.valueOf(it.name) }),
        topN,
        Order.valueOf(order.name)
    )
}
