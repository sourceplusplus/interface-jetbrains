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
package spp.jetbrains.sourcemarker.service

import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.portal.SourcePortal
import spp.jetbrains.sourcemarker.discover.TCPServiceDiscoveryBackend
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.search.SourceMarkSearch
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.ProtocolAddress
import spp.protocol.ProtocolAddress.Global.ArtifactMetricsUpdated
import spp.protocol.ProtocolAddress.Global.ArtifactTracesUpdated
import spp.protocol.ProtocolAddress.Global.TraceSpanUpdated
import spp.protocol.SourceServices.Provide.toLiveViewSubscriberAddress
import spp.protocol.artifact.ArtifactType
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.log.Log
import spp.protocol.artifact.log.LogOrderType
import spp.protocol.artifact.log.LogResult
import spp.protocol.artifact.metrics.ArtifactMetricResult
import spp.protocol.artifact.metrics.ArtifactMetrics
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.artifact.trace.Trace
import spp.protocol.artifact.trace.TraceOrderType
import spp.protocol.artifact.trace.TraceResult
import spp.protocol.artifact.trace.TraceSpan
import spp.protocol.instrument.event.LiveInstrumentEvent
import spp.protocol.instrument.event.LiveInstrumentEventType.METER_UPDATED
import spp.protocol.instrument.meter.MeterType
import spp.protocol.view.LiveViewEvent
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder

class LiveViewManager(private val pluginConfig: SourceMarkerConfig) : CoroutineVerticle() {

    private val log = LoggerFactory.getLogger(LiveViewManager::class.java)

    private val formatter = DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmm")
        .toFormatter()
        .withZone(ZoneOffset.UTC) //todo: load from SkywalkingMonitor

    override suspend fun start() {
        //register listener
        var developer = "system"
        if (pluginConfig.serviceToken != null) {
            val json = JWT.parse(pluginConfig.serviceToken)
            developer = json.getJsonObject("payload").getString("developer_id")
        }

        vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(developer)) {
            val event = Json.decodeValue(it.body().toString(), LiveViewEvent::class.java)
            if (log.isTraceEnabled) log.trace("Received live event: {}", event)
            if (!SourceMarker.enabled) {
                log.warn("SourceMarker is not enabled, ignoring live event: {}", event)
                return@consumer
            }

            when (event.viewConfig.viewName) {
                "LIVE_METER" -> launch(vertx.dispatcher()) { consumeLiveMeterEvent(event) }
                "LOGS" -> launch(vertx.dispatcher()) { consumeLogsViewEvent(event) }
                "TRACES" -> {
                    val sourceMark = SourceMarkSearch.findByEndpointName(event.entityId)
                    if (sourceMark == null) {
                        log.info("Could not find source mark for: " + event.entityId)
                        return@consumer
                    }
                    consumeTracesViewEvent(event, sourceMark)
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
            toLiveViewSubscriberAddress(developer), null,
            JsonObject().apply { pluginConfig.serviceToken?.let { put("auth-token", it) } },
            null, null, TCPServiceDiscoveryBackend.socket!!
        )
    }

    private fun consumeLiveMeterEvent(event: LiveViewEvent) {
        val meterTypeStr = event.entityId.substringAfter("spp_").substringBefore("_").toUpperCase()
        val meterType = MeterType.valueOf(meterTypeStr)
        val meterId = event.entityId.substringAfter(meterType.name.toLowerCase() + "_").replace("_", "-")
        val meterMark = SourceMarkSearch.findByInstrumentId(meterId)
        if (meterMark == null) {
            log.info("Could not find source mark for: " + event.entityId)
            return
        }

        //todo: event listener that works with LiveViewEvent and LiveInstrumentEvent
        val eventListeners = meterMark.getUserData(SourceMarkKeys.INSTRUMENT_EVENT_LISTENERS)
        if (eventListeners?.isNotEmpty() == true) {
            eventListeners.forEach { it.accept(LiveInstrumentEvent(METER_UPDATED, Json.encode(event))) }
        }
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

        if (event.artifactQualifiedName.type == ArtifactType.EXPRESSION) {
            val expressionMark = SourceMarkSearch.findSourceMark(event.artifactQualifiedName)
            if (expressionMark != null) {
                vertx.eventBus().send(ProtocolAddress.Global.ArtifactLogUpdated, logsResult)
            }
        } else {
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
    }

    private fun consumeTracesViewEvent(
        event: LiveViewEvent,
        sourceMark: SourceMark
    ) {
        val rawMetrics = JsonObject(event.metricsData)
        val traces = mutableListOf<Trace>()
        val trace = Json.decodeValue(rawMetrics.getJsonObject("trace").toString(), Trace::class.java)
        traces.add(trace)

        val traceResult = TraceResult(
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
            val updatedEndpointName = "$httpMethod:${URI(url).path}"
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
        val artifactMetrics = toArtifactMetrics(event)
        val metricResult = ArtifactMetricResult(
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

    private fun toArtifactMetrics(event: LiveViewEvent): List<ArtifactMetrics> {
        val rawMetrics = mutableListOf<Int>()
        if (event.viewConfig.viewMetrics.size > 1) {
            val multiMetrics = JsonArray(event.metricsData)
            for (i in 0 until multiMetrics.size()) {
                val metricsName = multiMetrics.getJsonObject(i).getJsonObject("meta").getString("metricsName")
                val value = when (MetricType.realValueOf(metricsName)) {
                    MetricType.Throughput_Average -> multiMetrics.getJsonObject(i)
                        .getInteger("value")
                    MetricType.ResponseTime_Average -> multiMetrics.getJsonObject(i)
                        .getInteger("value")
                    MetricType.ServiceLevelAgreement_Average -> multiMetrics.getJsonObject(i)
                        .getInteger("percentage")
                    else -> TODO(metricsName)
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
        return artifactMetrics
    }
}
