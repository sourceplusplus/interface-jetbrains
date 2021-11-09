package com.sourceplusplus.portal.extensions

import spp.protocol.ProtocolAddress.Portal.DisplayActivity
import spp.protocol.ProtocolAddress.Portal.DisplayLog
import spp.protocol.ProtocolAddress.Portal.DisplaySpanInfo
import spp.protocol.ProtocolAddress.Portal.DisplayTraceStack
import spp.protocol.ProtocolAddress.Portal.DisplayTraces
import spp.protocol.artifact.log.Log
import spp.protocol.artifact.metrics.ArtifactMetricResult
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpan
import spp.protocol.artifact.trace.TraceStackPath
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
