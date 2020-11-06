package com.sourceplusplus.sourcemarker.listeners

import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.ProjectManager
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkJcefComponent
import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.average
import com.sourceplusplus.monitor.skywalking.bridge.EndpointMetricsBridge
import com.sourceplusplus.monitor.skywalking.bridge.EndpointTracesBridge
import com.sourceplusplus.monitor.skywalking.model.GetEndpointMetrics
import com.sourceplusplus.monitor.skywalking.model.GetEndpointTraces
import com.sourceplusplus.monitor.skywalking.model.ZonedDuration
import com.sourceplusplus.monitor.skywalking.toProtocol
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.extensions.fromPerSecondToPrettyFrequency
import com.sourceplusplus.portal.extensions.toPrettyDuration
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactMetricUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactTraceUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedStackTraceElement
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClosePortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.FindAndOpenPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.GetPortalConfiguration
import com.sourceplusplus.protocol.ProtocolAddress.Global.NavigateToArtifact
import com.sourceplusplus.protocol.ProtocolAddress.Global.OpenPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.QueryTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshActivity
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshOverview
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshTraces
import com.sourceplusplus.protocol.ProtocolAddress.Portal.UpdateEndpoints
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.endpoint.EndpointResult
import com.sourceplusplus.protocol.artifact.endpoint.EndpointType
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.protocol.artifact.metrics.ArtifactSummarizedMetrics
import com.sourceplusplus.protocol.artifact.metrics.ArtifactSummarizedResult
import com.sourceplusplus.protocol.artifact.metrics.MetricType
import com.sourceplusplus.sourcemarker.SourceMarkKeys
import com.sourceplusplus.sourcemarker.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.navigate.ArtifactNavigator
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.text.DecimalFormat
import java.time.ZonedDateTime
import javax.swing.UIManager

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PortalEventListener : CoroutineVerticle() {

    private var lastDisplayedInternalPortal: SourcePortal? = null

    override suspend fun start() {
        //listen for theme changes
        UIManager.addPropertyChangeListener {
            val darkMode = (it.newValue !is IntelliJLaf)
            //todo: update existing portals
        }

        vertx.eventBus().consumer<String>(GetPortalConfiguration) {
            val portalUuid = it.body()
            val portal = SourcePortal.getPortal(portalUuid)!!
            it.reply(JsonObject.mapFrom(portal.configuration))
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(FindAndOpenPortal) {
            val artifactQualifiedName = it.body()
            runReadAction {
                val sourceMarks = SourceMarker.getSourceMarks(artifactQualifiedName.identifier)
                if (sourceMarks.isNotEmpty()) {
                    openPortal(sourceMarks[0].getUserData(SourceMarkKeys.SOURCE_PORTAL)!!)
                }
            }
        }
        vertx.eventBus().consumer<SourcePortal>(OpenPortal) { openPortal(it.body()) }
        vertx.eventBus().consumer<SourcePortal>(ClosePortal) { closePortal(it.body()) }
        vertx.eventBus().consumer<JsonObject>(RefreshOverview) {
            val portalUuid = it.body().getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            runReadAction {
                val fileMarker = SourceMarker.getSourceFileMarker(portal.viewingPortalArtifact)!!
                GlobalScope.launch(vertx.dispatcher()) {
                    refreshOverview(fileMarker, portal)
                }
            }
        }
        vertx.eventBus().consumer<SourcePortal>(RefreshActivity) {
            GlobalScope.launch(vertx.dispatcher()) {
                refreshActivity(it.body())
            }
        }
        vertx.eventBus().consumer<SourcePortal>(RefreshTraces) {
            GlobalScope.launch(vertx.dispatcher()) {
                refreshTraces(it.body())
            }
        }
        vertx.eventBus().consumer<String>(QueryTraceStack) { handler ->
            val traceId = handler.body()
            GlobalScope.launch(vertx.dispatcher()) {
                handler.reply(EndpointTracesBridge.getTraceStack(traceId, vertx))
            }
        }
        vertx.eventBus().consumer<JsonObject>(ClickedStackTraceElement) { handler ->
            val message = handler.body()
            val portalUuid = message.getString("portalUuid")
            val portal = SourcePortal.getPortal(portalUuid)!!
            if (!portal.configuration.external) vertx.eventBus().send(ClosePortal, portal)

            val element = Json.decodeValue(
                message.getJsonObject("stackTraceElement").toString(),
                JvmStackTraceElement::class.java
            )
            val project = ProjectManager.getInstance().openProjects[0]
            ArtifactNavigator.navigateTo(project, element)
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(NavigateToArtifact) {
            val artifactQualifiedName = it.body()
            val project = ProjectManager.getInstance().openProjects[0]
            ArtifactNavigator.navigateTo(project, artifactQualifiedName)
        }
    }

    private suspend fun refreshTraces(portal: SourcePortal) {
        val sourceMark =
            SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null && sourceMark is MethodSourceMark) {
            val endpointId = sourceMark.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(sourceMark)
            if (endpointId != null) {
                GlobalScope.launch(vertx.dispatcher()) {
                    val traceResult = EndpointTracesBridge.getTraces(
                        GetEndpointTraces(
                            appUuid = portal.appUuid,
                            artifactQualifiedName = portal.viewingPortalArtifact,
                            endpointId = endpointId,
                            zonedDuration = ZonedDuration(
                                ZonedDateTime.now().minusMinutes(15),
                                ZonedDateTime.now(),
                                SkywalkingClient.DurationStep.MINUTE
                            ),
                            orderType = portal.tracesView.orderType
                        ), vertx
                    )
                    vertx.eventBus().send(ArtifactTraceUpdated, traceResult)
                }
            }
        }
    }

    private suspend fun refreshOverview(fileMarker: SourceFileMarker, portal: SourcePortal) {
        val endpointMarks = fileMarker.getSourceMarks().filterIsInstance<MethodSourceMark>()
            .filter {
                it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(it) != null
            }

        val fetchMetricTypes = listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla")
        val requestDuration = ZonedDuration(
            ZonedDateTime.now().minusMinutes(portal.overviewView.timeFrame.minutes.toLong()),
            ZonedDateTime.now(),
            SkywalkingClient.DurationStep.MINUTE
        )
        val endpointMetricResults = mutableListOf<ArtifactSummarizedResult>()
        endpointMarks.forEach {
            val metricsRequest = GetEndpointMetrics(
                fetchMetricTypes,
                it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(it)!!,
                requestDuration
            )
            val metrics = EndpointMetricsBridge.getMetrics(metricsRequest, vertx)
            val endpointName = it.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointName(it)!!

            val summarizedMetrics = mutableListOf<ArtifactSummarizedMetrics>()
            for (i in metrics.indices) {
                val avg = metrics[i].values.average()
                val metricType = MetricType.realValueOf(fetchMetricTypes[i])
                val summaryValue = when (metricType) {
                    MetricType.Throughput_Average -> (avg / 60.0).fromPerSecondToPrettyFrequency()
                    MetricType.ResponseTime_Average -> avg.toInt().toPrettyDuration()
                    MetricType.ServiceLevelAgreement_Average -> {
                        if (avg == 0.0) "0%" else DecimalFormat(".#").format(avg / 100.0).toString() + "%"
                    }
                    else -> throw UnsupportedOperationException(fetchMetricTypes[i])
                }
                summarizedMetrics.add(ArtifactSummarizedMetrics(metricType, summaryValue))
            }

            endpointMetricResults.add(
                ArtifactSummarizedResult(
                    ArtifactQualifiedName(
                        it.artifactQualifiedName,
                        "todo",
                        ArtifactType.ENDPOINT,
                        operationName = endpointName
                    ),
                    summarizedMetrics,
                    EndpointType.HTTP
                )
            )
        }

        vertx.eventBus().send(
            UpdateEndpoints(portal.portalUuid),
            JsonObject(
                Json.encode(
                    EndpointResult(
                        portal.appUuid, portal.overviewView.timeFrame,
                        start = Instant.fromEpochMilliseconds(requestDuration.start.toInstant().toEpochMilli()),
                        stop = Instant.fromEpochMilliseconds(requestDuration.stop.toInstant().toEpochMilli()),
                        step = requestDuration.step.name,
                        endpointMetricResults
                    )
                )
            )
        )
    }

    private suspend fun refreshActivity(portal: SourcePortal) {
        val sourceMark =
            SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null && sourceMark is MethodSourceMark) {
            val endpointId = sourceMark.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(sourceMark)
            if (endpointId != null) {
                val metricsRequest = GetEndpointMetrics(
                    listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla"),
                    endpointId,
                    ZonedDuration(
                        ZonedDateTime.now().minusMinutes(portal.activityView.timeFrame.minutes.toLong()),
                        ZonedDateTime.now(),
                        SkywalkingClient.DurationStep.MINUTE
                    )
                )
                val metrics = EndpointMetricsBridge.getMetrics(metricsRequest, vertx)
                val metricResult = toProtocol(
                    portal.appUuid,
                    portal.viewingPortalArtifact,
                    portal.activityView.timeFrame,
                    metricsRequest,
                    metrics
                )

                val finalArtifactMetrics = metricResult.artifactMetrics.toMutableList()
//                val multipleMetricsRequest = GetMultipleEndpointMetrics(
//                    "endpoint_percentile",
//                    endpointId,
//                    5,
//                    ZonedDuration(
//                        ZonedDateTime.now().minusMinutes(portal.activityView.timeFrame.minutes.toLong()),
//                        ZonedDateTime.now(),
//                        SkywalkingClient.DurationStep.MINUTE
//                    )
//                )
//                val multiMetrics = EndpointMetricsBridge.getMultipleMetrics(
//                    multipleMetricsRequest, vertx
//                )
//                multiMetrics.forEachIndexed { i, it ->
//                    finalArtifactMetrics.add(
//                        ArtifactMetrics(
//                            metricType = when (i) {
//                                0 -> MetricType.ResponseTime_50Percentile
//                                1 -> MetricType.ResponseTime_75Percentile
//                                2 -> MetricType.ResponseTime_90Percentile
//                                3 -> MetricType.ResponseTime_95Percentile
//                                4 -> MetricType.ResponseTime_99Percentile
//                                else -> throw IllegalStateException()
//                            },
//                            values = it.values.map { it.toProtocol() }
//                        )
//                    )
//                }

                vertx.eventBus().send(
                    ArtifactMetricUpdated, metricResult.copy(
                        artifactMetrics = finalArtifactMetrics
                    )
                )
            }
        }
    }

    private fun openPortal(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null) {
            ApplicationManager.getApplication().invokeLater(sourceMark::displayPopup)

            val jcefComponent = sourceMark.sourceMarkComponent as SourceMarkJcefComponent
            if (portal != lastDisplayedInternalPortal) {
                portal.configuration.darkMode = UIManager.getLookAndFeel() !is IntelliJLaf
                val host = "http://localhost:8080"
                val currentUrl = "$host/${portal.currentTab.name.toLowerCase()}.html" +
                        "?portalUuid=${portal.portalUuid}"

                if (lastDisplayedInternalPortal == null) {
                    jcefComponent.configuration.initialUrl = currentUrl
                } else {
                    jcefComponent.getBrowser().cefBrowser.executeJavaScript(
                        "window.location.href = '$currentUrl';", currentUrl, 0
                    )
                }
                lastDisplayedInternalPortal = portal
            }
        }
    }

    private fun closePortal(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null) {
            ApplicationManager.getApplication().invokeLater(sourceMark::closePopup)
        }
    }
}
