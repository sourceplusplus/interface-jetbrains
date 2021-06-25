package com.sourceplusplus.portal.extensions

import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayActivity
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayLog
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayLogs
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayTraces
import com.sourceplusplus.protocol.artifact.log.Log
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.trace.TraceStackPath
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

//todo: everything requiring portalUuid could probably be moved to SourcePortal

fun EventBus.displayActivity(portalUuid: String, metricResult: ArtifactMetricResult) {
    send(DisplayActivity(portalUuid), JsonObject(Json.encode(metricResult)))
}

fun EventBus.displayTraces(portalUuid: String, traceResult: TraceResult) {
    send(DisplayTraces(portalUuid), JsonObject(Json.encode(traceResult)))
}

fun EventBus.displayLog(portalUuid: String, log: Log) {
    send(DisplayLog(portalUuid), JsonObject(Json.encode(log)))
}

fun EventBus.displayTraceSpan(portalUuid: String, span: TraceSpan) {
    send(DisplaySpanInfo(portalUuid), JsonObject(Json.encode(span)))
}

@Deprecated("")
fun EventBus.displayTraceSpan(portalUuid: String, span: JsonObject) {
    send(DisplaySpanInfo(portalUuid), span)
}

fun EventBus.displayTraceStack(portalUuid: String, traceStackPath: TraceStackPath) {
    send(DisplayTraceStack(portalUuid), JsonObject(Json.encode(traceStackPath)))
}
