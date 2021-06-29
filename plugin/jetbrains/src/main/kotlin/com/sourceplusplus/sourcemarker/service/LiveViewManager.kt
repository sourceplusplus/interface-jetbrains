package com.sourceplusplus.sourcemarker.service

import com.intellij.openapi.project.Project
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.ProtocolAddress
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactMetricsUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactTracesUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.TraceSpanUpdated
import com.sourceplusplus.protocol.SourceMarkerServices.Provide
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.log.Log
import com.sourceplusplus.protocol.artifact.log.LogOrderType
import com.sourceplusplus.protocol.artifact.log.LogResult
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetrics
import com.sourceplusplus.protocol.artifact.metrics.MetricType
import com.sourceplusplus.protocol.artifact.trace.Trace
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.view.LiveViewEvent
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin.INSTANCE_ID
import com.sourceplusplus.sourcemarker.discover.TCPServiceDiscoveryBackend
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.search.SourceMarkSearch
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder

class LiveViewManager(private val project: Project) : CoroutineVerticle() {

    private val log = LoggerFactory.getLogger(LiveViewManager::class.java)

    private val formatter = DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmm")
        .toFormatter()
        .withZone(ZoneOffset.UTC) //todo: load from SkywalkingMonitor

    override suspend fun start() {
        //register listener
        vertx.eventBus().consumer<JsonObject>("local." + Provide.LIVE_VIEW_SUBSCRIBER + "." + INSTANCE_ID) {
            val event = Json.decodeValue(it.body().toString(), LiveViewEvent::class.java)
            if (log.isTraceEnabled) log.trace("Received live event: {}", event)

            when (event.viewConfig.viewName) {
                "LOGS" -> GlobalScope.launch(vertx.dispatcher()) { consumeLogsViewEvent(event) }
                "TRACES" -> {
                    val sourceMark = SourceMarkSearch.findByEndpointName(event.entityId)
                    if (sourceMark == null) {
                        log.info("Could not find source mark for: " + event.entityId)
                        return@consumer
                    }
                    val portal = sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                    consumeTracesViewEvent(event, sourceMark, portal)
                }
                else -> {
                    val sourceMark = SourceMarkSearch.findByEndpointName(event.entityId)
                    if (sourceMark == null) {
                        log.info("Could not find source mark for: " + event.entityId)
                        return@consumer
                    }
                    val portal = sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                    consumeActivityViewEvent(event, sourceMark, portal)
                }
            }
        }

        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.toLowerCase(),
            Provide.LIVE_VIEW_SUBSCRIBER + "." + INSTANCE_ID,
            JsonObject(),
            TCPServiceDiscoveryBackend.socket!!
        )
    }

    private suspend fun consumeLogsViewEvent(event: LiveViewEvent) {
        val rawMetrics = JsonObject(event.metricsData)
        val logData = Json.decodeValue(rawMetrics.getJsonObject("log").toString(), Log::class.java)
        val logsResult = LogResult(
            event.artifactQualifiedName,
            LogOrderType.NEWEST_LOGS,
            logData.timestamp,
            listOf(logData),
            Int.MAX_VALUE
        )

        for ((content, logs) in logsResult.logs.groupBy { it.content }) {
            SourceMarkSearch.findInheritedSourceMarks(content).forEach {
                vertx.eventBus().send(
                    ProtocolAddress.Global.ArtifactLogUpdated,
                    logsResult.copy(
                        artifactQualifiedName = it.artifactQualifiedName,
                        total = logs.size,
                        logs = logs,
                    )
                )
            }
        }
    }

    private fun consumeTracesViewEvent(
        event: LiveViewEvent,
        sourceMark: SourceMark,
        portal: SourcePortal
    ) {
        val rawMetrics = JsonObject(event.metricsData)
        val traces = mutableListOf<Trace>()
        val trace = Json.decodeValue(rawMetrics.getJsonObject("trace").toString(), Trace::class.java)
        traces.add(trace)

        val traceResult = TraceResult(
            "null",
            sourceMark.artifactQualifiedName,
            null,
            TraceOrderType.LATEST_TRACES,
            trace.start,
            trace.start.toJavaInstant().minusMillis(trace.duration.toLong()).toKotlinInstant(),
            "minute",
            traces,
            Int.MAX_VALUE
        )
        vertx.eventBus().send(ArtifactTracesUpdated, traceResult)

        val url = trace.meta["url"]
        val httpMethod = trace.meta["http.method"]
        if (url != null && httpMethod != null) {
            val updatedEndpointName = "{$httpMethod}${URI(url).path}"
            val entrySpan = Json.decodeValue(trace.meta["entrySpan"], TraceSpan::class.java)
            vertx.eventBus().send(
                TraceSpanUpdated, entrySpan.copy(
                    endpointName = updatedEndpointName,
                    artifactQualifiedName = event.artifactQualifiedName
                )
            )
        }
    }

    private fun consumeActivityViewEvent(
        event: LiveViewEvent,
        sourceMark: SourceMark,
        portal: SourcePortal
    ) {
        val rawMetrics = mutableListOf<Int>()
        if (event.viewConfig.viewMetrics.size > 1) {
            val multiMetrics = JsonArray(event.metricsData)
            event.viewConfig.viewMetrics.forEachIndexed { i, it ->
                val value = when (val metricType = MetricType.realValueOf(it)) {
                    MetricType.Throughput_Average -> multiMetrics.getJsonObject(i)
                        .getInteger("value")
                    MetricType.ResponseTime_Average -> multiMetrics.getJsonObject(i)
                        .getInteger("value")
                    MetricType.ServiceLevelAgreement_Average -> multiMetrics.getJsonObject(i)
                        .getInteger("percentage")
                    else -> TODO(metricType.name)
                }
                rawMetrics.add(value)
            }
        } else {
            val value = when (val metricType = MetricType.realValueOf(event.viewConfig.viewMetrics.first())) {
                MetricType.Throughput_Average -> JsonObject(event.metricsData).getInteger("value")
                MetricType.ResponseTime_Average -> JsonObject(event.metricsData).getInteger("value")
                MetricType.ServiceLevelAgreement_Average -> JsonObject(event.metricsData).getInteger("percentage")
                else -> TODO(metricType.name)
            }
            rawMetrics.add(value)
        }
        val artifactMetrics = rawMetrics.mapIndexed { i: Int, it: Int ->
            ArtifactMetrics(MetricType.realValueOf(event.viewConfig.viewMetrics[i]), listOf(it.toDouble()))
        }

        val metricResult = ArtifactMetricResult(
            "null",
            sourceMark.artifactQualifiedName,
            QueryTimeFrame.valueOf(1),
            portal.activityView.activeChartMetric, //todo: assumes activity view
            formatter.parse(event.timeBucket, Instant::from).toKotlinInstant(),
            formatter.parse(event.timeBucket, Instant::from).plusSeconds(60).toKotlinInstant(),
            "minute",
            artifactMetrics,
            true
        )
        vertx.eventBus().send(ArtifactMetricsUpdated, metricResult)
    }
}
