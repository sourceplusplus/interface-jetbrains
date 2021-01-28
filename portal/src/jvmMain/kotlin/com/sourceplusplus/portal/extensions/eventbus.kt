package com.sourceplusplus.portal.extensions

import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayCard
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayLogs
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayTraces
import com.sourceplusplus.protocol.ProtocolAddress.Portal.UpdateChart
import com.sourceplusplus.protocol.artifact.log.LogResult
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.artifact.metrics.BarTrendCard
import com.sourceplusplus.protocol.artifact.metrics.SplineChart
import com.sourceplusplus.protocol.artifact.trace.TraceStackPath
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

//todo: everything requiring portalUuid could probably be moved to SourcePortal

fun EventBus.updateChart(portalUuid: String, splineChart: SplineChart) {
    send(UpdateChart(portalUuid), JsonObject(Json.encode(splineChart)))
}

fun EventBus.displayTraces(portalUuid: String, traceResult: TraceResult) {
    send(DisplayTraces(portalUuid), JsonObject(Json.encode(traceResult)))
}

fun EventBus.displayLogs(portalUuid: String, logResult: LogResult) {
    send(DisplayLogs(portalUuid), JsonObject(Json.encode(logResult)))
}

fun EventBus.displayCard(portalUuid: String, card: BarTrendCard) {
    send(DisplayCard(portalUuid), JsonObject(Json.encode(card)))
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
