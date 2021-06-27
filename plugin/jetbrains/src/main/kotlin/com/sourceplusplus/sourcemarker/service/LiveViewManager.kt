package com.sourceplusplus.sourcemarker.service

import com.intellij.openapi.project.Project
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactMetricsUpdated
import com.sourceplusplus.protocol.SourceMarkerServices.Provide
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetrics
import com.sourceplusplus.protocol.artifact.metrics.MetricType
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
import kotlinx.datetime.toKotlinInstant
import org.slf4j.LoggerFactory
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
//        FrameHelper.sendFrame(
//            BridgeEventType.REGISTER.name.toLowerCase(),
//            SourceMarkerServices.Provide.LIVE_VIEW_SUBSCRIBER,
//            JsonObject(),
//            TCPServiceDiscoveryBackend.socket!!
//        )

        vertx.eventBus().consumer<JsonObject>("local." + Provide.LIVE_VIEW_SUBSCRIBER + "." + INSTANCE_ID) {
            val event = Json.decodeValue(it.body().toString(), LiveViewEvent::class.java)
            if (log.isTraceEnabled) {
                log.trace("Received live event: {}", event)
            }

            val sourceMark = SourceMarkSearch.findByEndpointName(event.entityId)
            if (sourceMark == null) {
                log.info("Could not find source mark for: " + event.entityId)
                return@consumer
            }
            val portal = sourceMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!

            val rawMetrics = mutableListOf<Int>()
            if (event.metricNames.size > 1) {
                val multiMetrics = JsonArray(event.metricsData)
                event.metricNames.forEachIndexed { i, it ->
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
                val value = when (val metricType = MetricType.realValueOf(event.metricNames.first())) {
                    MetricType.Throughput_Average -> JsonObject(event.metricsData).getInteger("value")
                    MetricType.ResponseTime_Average -> JsonObject(event.metricsData).getInteger("value")
                    MetricType.ServiceLevelAgreement_Average -> JsonObject(event.metricsData).getInteger("percentage")
                    else -> TODO(metricType.name)
                }
                rawMetrics.add(value)
            }
            val artifactMetrics = rawMetrics.mapIndexed { i: Int, it: Int ->
                ArtifactMetrics(MetricType.realValueOf(event.metricNames.get(i)), listOf(it.toDouble()))
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

        FrameHelper.sendFrame(
            BridgeEventType.REGISTER.name.toLowerCase(),
            Provide.LIVE_VIEW_SUBSCRIBER + "." + INSTANCE_ID,
            JsonObject(),
            TCPServiceDiscoveryBackend.socket!!
        )
    }
}
