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
package spp.jetbrains.sourcemarker.view

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.psi.EndpointDetector
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode.CUSTOM_EVENT
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkVirtualText
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.bridge.EndpointMetricsBridge
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.jetbrains.monitor.skywalking.toProtocol
import spp.jetbrains.sourcemarker.PluginBundle
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.jetbrains.sourcemarker.PluginUI
import spp.jetbrains.sourcemarker.SourceMarkerPlugin.vertx
import spp.jetbrains.sourcemarker.command.LiveControlCommand.HIDE_QUICK_STATS
import spp.jetbrains.sourcemarker.command.LiveControlCommand.SHOW_QUICK_STATS
import spp.jetbrains.sourcemarker.mark.SourceMarkSearch
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.SourceServices
import spp.protocol.SourceServices.Provide.toLiveViewSubscriberAddress
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.metrics.ArtifactMetricResult
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.instrument.LiveSourceLocation
import spp.protocol.utils.fromPerSecondToPrettyFrequency
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import spp.protocol.view.LiveViewSubscription
import java.awt.Color
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Adds activity quick stats as inlay marks above recognized endpoint methods.
 * Uses a two-minute delay to ensure metrics have been fully collected.
 */
class ActivityQuickStatsIndicator(val config: SourceMarkerConfig) : SourceMarkEventListener {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ActivityQuickStatsIndicator::class.java)
        private val inlayForegroundColor = JBColor(Color.decode("#3e464a"), Color.decode("#87939a"))
        val SHOWING_QUICK_STATS = SourceKey<Boolean>("SHOWING_QUICK_STATS")
    }

    override fun handleEvent(event: SourceMarkEvent) {
        if (config.autoDisplayEndpointQuickStats && event.eventCode == SourceMarkEventCode.MARK_USER_DATA_UPDATED) {
            if (event.sourceMark.getUserData(EndpointDetector.ENDPOINT_ID) != null) {
                val existingMarks = SourceMarkSearch.findSourceMarks(event.sourceMark.artifactQualifiedName)
                if (existingMarks.find { it.getUserData(SHOWING_QUICK_STATS) == true } != null) return

                displayQuickStatsInlay(event.sourceMark)
            }
        } else if (event.eventCode == CUSTOM_EVENT && event.params.first() == SHOW_QUICK_STATS) {
            if (event.sourceMark.getUserData(EndpointDetector.ENDPOINT_ID) != null) {
                displayQuickStatsInlay(event.sourceMark)
            }
        } else if (event.eventCode == CUSTOM_EVENT && event.params.first() == HIDE_QUICK_STATS) {
            val existingQuickStats = event.sourceMark.sourceFileMarker.getSourceMarks().find {
                it.artifactQualifiedName == event.sourceMark.artifactQualifiedName
                        && it.getUserData(SHOWING_QUICK_STATS) == true
            }
            existingQuickStats?.dispose()
        }
    }

    private fun displayQuickStatsInlay(sourceMark: SourceMark) = ApplicationManager.getApplication().runReadAction {
        log.info("Displaying quick stats inlay for {}", sourceMark.artifactQualifiedName.identifier)
        val endTime = ZonedDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES) //exclusive
        val startTime = endTime.minusMinutes(2)
        val metricsRequest = GetEndpointMetrics(
            listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla"),
            sourceMark.getUserData(EndpointDetector.ENDPOINT_ID)!!,
            ZonedDuration(startTime, endTime, SkywalkingClient.DurationStep.MINUTE)
        )

        val currentMetrics = runBlocking(vertx.dispatcher()) { EndpointMetricsBridge.getMetrics(metricsRequest, vertx) }
        val metricResult = toProtocol(
            sourceMark.artifactQualifiedName,
            QueryTimeFrame.LAST_15_MINUTES, //todo: don't need
            MetricType.ResponseTime_Average, //todo: dont need
            metricsRequest,
            currentMetrics
        )

        val inlay = SourceMarker.creationService.createMethodInlayMark(
            sourceMark.sourceFileMarker,
            (sourceMark as MethodSourceMark).getPsiElement().nameIdentifier!!,
            false
        )
        inlay.putUserData(SHOWING_QUICK_STATS, true)
        inlay.configuration.virtualText = InlayMarkVirtualText(inlay, formatMetricResult(metricResult))
        inlay.configuration.virtualText!!.textAttributes.foregroundColor = inlayForegroundColor
        if (PluginBundle.LOCALE.language == "zh") {
            inlay.configuration.virtualText!!.font = PluginUI.MICROSOFT_YAHEI_PLAIN_14
            inlay.configuration.virtualText!!.xOffset = 15
        }
        inlay.configuration.activateOnMouseClick = false
        inlay.apply(true)

        SourceServices.Instance.liveView!!.addLiveViewSubscription(
            LiveViewSubscription(
                null,
                listOf(sourceMark.getUserData(EndpointDetector.ENDPOINT_NAME)!!),
                sourceMark.artifactQualifiedName.copy(
                    operationName = sourceMark.getUserData(EndpointDetector.ENDPOINT_ID)!! //todo: only SWLiveViewService uses
                ),
                LiveSourceLocation(sourceMark.artifactQualifiedName.identifier, 0), //todo: don't need
                LiveViewConfig("ACTIVITY", listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla"), -1)
            )
        ).onComplete {
            if (it.succeeded()) {
                val subscriptionId = it.result().subscriptionId!!
                val previousMetrics = mutableMapOf<Long, String>()
                vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(subscriptionId)) {
                    val viewEvent = Json.decodeValue(it.body().toString(), LiveViewEvent::class.java)
                    consumeLiveEvent(viewEvent, previousMetrics)

                    val twoMinAgoValue = previousMetrics[viewEvent.timeBucket.toLong() - 2]
                    if (twoMinAgoValue != null) {
                        inlay.configuration.virtualText!!.updateVirtualText(twoMinAgoValue)
                    }
                }
                inlay.addEventListener {
                    if (it.eventCode == SourceMarkEventCode.MARK_REMOVED) {
                        SourceServices.Instance.liveView!!.removeLiveViewSubscription(subscriptionId)
                    }
                }
            } else {
                log.error("Failed to add live view subscription", it.cause())
            }
        }
    }

    private fun formatMetricResult(result: ArtifactMetricResult): String {
        val sb = StringBuilder()
        val resp = result.artifactMetrics.find { it.metricType == MetricType.Throughput_Average }!!
        val respValue = (resp.values.last() / 60.0).fromPerSecondToPrettyFrequency({ message(it) })
        sb.append(message(resp.metricType.simpleName)).append(": ").append(respValue).append(" | ")
        val cpm = result.artifactMetrics.find { it.metricType == MetricType.ResponseTime_Average }!!
        sb.append(message(cpm.metricType.simpleName)).append(": ").append(cpm.values.last().toInt()).append(message("ms")).append(" | ")
        val sla = result.artifactMetrics.find { it.metricType == MetricType.ServiceLevelAgreement_Average }!!
        sb.append(message(sla.metricType.simpleName)).append(": ").append(sla.values.last().toDouble() / 100.0).append("%")
        return "/#/ " + sb.toString() + " \\#\\"
    }

    private fun consumeLiveEvent(event: LiveViewEvent, previousMetrics: MutableMap<Long, String>) {
        val metrics = JsonArray(event.metricsData)
        val sb = StringBuilder()
        for (i in 0 until metrics.size()) {
            val metric = metrics.getJsonObject(i)
            var value: String? = null
            if (metric.getNumber("percentage") != null) {
                value = (metric.getNumber("percentage").toDouble() / 100.0).toString() + "%"
            }
            if (value == null) value = metric.getNumber("value").toString()

            val metricType = MetricType.realValueOf(metric.getJsonObject("meta").getString("metricsName"))
            if (metricType == MetricType.Throughput_Average) {
                value = (metric.getNumber("value").toDouble() / 60.0).fromPerSecondToPrettyFrequency({ message(it) })
            }
            if (metricType == MetricType.ResponseTime_Average) {
                value += message("ms")
            }
            sb.append("${message(metricType.simpleName)}: $value")
            if (i < metrics.size() - 1) {
                sb.append(" | ")
            }
        }
        previousMetrics[event.timeBucket.toLong()] = "/#/ " + sb.toString() + " \\#\\"
    }
}
