package com.sourceplusplus.monitor.skywalking

import com.sourceplusplus.monitor.skywalking.model.GetEndpointMetrics
import com.sourceplusplus.protocol.artifact.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.ArtifactMetrics
import com.sourceplusplus.protocol.artifact.trace.*
import com.sourceplusplus.protocol.portal.MetricType
import com.sourceplusplus.protocol.portal.QueryTimeFrame
import kotlinx.datetime.Instant
import monitor.skywalking.protocol.metrics.GetLinearIntValuesQuery
import monitor.skywalking.protocol.metrics.GetMultipleLinearIntValuesQuery
import monitor.skywalking.protocol.trace.QueryBasicTracesQuery
import monitor.skywalking.protocol.trace.QueryTraceQuery
import monitor.skywalking.protocol.type.QueryOrder
import monitor.skywalking.protocol.type.TraceState
import java.math.BigDecimal

fun toProtocol(
    appUuid: String,
    artifactQualifiedName: String,
    timeFrame: QueryTimeFrame,
    metricsRequest: GetEndpointMetrics,
    metrics: List<GetLinearIntValuesQuery.Result>
): ArtifactMetricResult {
    return ArtifactMetricResult(
        appUuid = appUuid,
        artifactQualifiedName = artifactQualifiedName,
        timeFrame = timeFrame,
        start = Instant.fromEpochMilliseconds(metricsRequest.zonedDuration.start.toInstant().toEpochMilli()),
        stop = Instant.fromEpochMilliseconds(metricsRequest.zonedDuration.stop.toInstant().toEpochMilli()),
        step = metricsRequest.zonedDuration.step.name,
        artifactMetrics = metrics.mapIndexed { i, result -> result.toProtocol(metricsRequest.metricIds[i]) }
    )
}

fun GetLinearIntValuesQuery.Result.toProtocol(metricType: String): ArtifactMetrics {
    return ArtifactMetrics(
        metricType = MetricType.realValueOf(metricType),
        values = values.map { (it.value as BigDecimal).toInt() }
    )
}

fun GetMultipleLinearIntValuesQuery.Value.toProtocol(): Int {
    return (value as BigDecimal).toInt()
}

fun GetLinearIntValuesQuery.Result.toDoubleArray(): DoubleArray {
    return values.map { (it.value as BigDecimal).toDouble() }.toDoubleArray()
}

fun QueryBasicTracesQuery.Trace.toProtocol(): Trace {
    return Trace(
        segmentId = segmentId,
        operationNames = endpointNames,
        duration = duration,
        start = start.toLong(),
        error = isError,
        traceIds = traceIds
    )
}

//todo: correctly
fun QueryTraceQuery.Log.toProtocol(): TraceSpanLogEntry {
    if (data!!.find { it.key == "stack" } != null) {
        return TraceSpanLogEntry(
            time = Instant.fromEpochMilliseconds((time as BigDecimal).toLong()).epochSeconds,
            data = data.find { it.key == "stack" }!!.value!! //todo: correctly
        )
    }
    return TraceSpanLogEntry(
        time = Instant.fromEpochMilliseconds((time as BigDecimal).toLong()).epochSeconds,
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
        startTime = (startTime as BigDecimal).toLong(),
        endTime = (endTime as BigDecimal).toLong(),
        endpointName = endpointName,
        type = type,
        peer = peer,
        component = component,
        error = isError,
        layer = layer,
        tags = tags.map { it.key to it.value!! }.toMap(),
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