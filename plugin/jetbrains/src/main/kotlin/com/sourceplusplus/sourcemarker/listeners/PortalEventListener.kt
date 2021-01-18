package com.sourceplusplus.sourcemarker.listeners

import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNameIdentifierOwner
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceMarkerUtils
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
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactMetricUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactTraceUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.CanNavigateToArtifact
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedStackTraceElement
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClosePortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.FindAndOpenPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.FindPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.GetPortalConfiguration
import com.sourceplusplus.protocol.ProtocolAddress.Global.NavigateToArtifact
import com.sourceplusplus.protocol.ProtocolAddress.Global.OpenPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.QueryTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshActivity
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshOverview
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetCurrentPage
import com.sourceplusplus.protocol.ProtocolAddress.Portal.UpdateEndpoints
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.endpoint.EndpointResult
import com.sourceplusplus.protocol.artifact.endpoint.EndpointType
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.protocol.artifact.metrics.ArtifactSummarizedMetrics
import com.sourceplusplus.protocol.artifact.metrics.ArtifactSummarizedResult
import com.sourceplusplus.protocol.artifact.metrics.MetricType
import com.sourceplusplus.protocol.utils.ArtifactNameUtils.getQualifiedClassName
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.navigate.ArtifactNavigator
import com.sourceplusplus.sourcemarker.search.ArtifactSearch.findArtifact
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
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

        vertx.eventBus().consumer<Any>(RefreshPortal) {
            val portal = if (it.body() is String) {
                SourcePortal.getPortal(it.body() as String)
            } else {
                it.body() as SourcePortal
            }!!
            when (portal.configuration.currentPage) {
                PageType.OVERVIEW -> vertx.eventBus().send(RefreshOverview, portal)
                PageType.ACTIVITY -> vertx.eventBus().send(RefreshActivity, portal)
                PageType.TRACES -> vertx.eventBus().send(RefreshTraces, portal)
                PageType.CONFIGURATION -> TODO()
            }
        }
        vertx.eventBus().consumer<JsonObject>(SetCurrentPage) {
            val portalUuid = it.body().getString("portalUuid")
            val pageType = PageType.valueOf(it.body().getString("pageType"))
            val portal = SourcePortal.getPortal(portalUuid)!!
            portal.configuration.currentPage = pageType
            it.reply(JsonObject.mapFrom(portal.configuration))
            vertx.eventBus().send(RefreshPortal, portal)
        }
        vertx.eventBus().consumer<String>(GetPortalConfiguration) {
            val portalUuid = it.body()
            val portal = SourcePortal.getPortal(portalUuid)!!
            it.reply(JsonObject.mapFrom(portal.configuration))
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(FindPortal) {
            val artifactQualifiedName = it.body()
            val sourceMarks = SourceMarker.getSourceMarks(artifactQualifiedName.identifier)
            if (sourceMarks.isNotEmpty()) {
                it.reply(sourceMarks[0].getUserData(SourceMarkKeys.SOURCE_PORTAL)!!)
            } else {
                GlobalScope.launch(vertx.dispatcher()) {
                    val classArtifact = findArtifact(artifactQualifiedName.copy(type = ArtifactType.CLASS))
                    val fileMarker = SourceMarker.getSourceFileMarker((classArtifact as PsiClass).containingFile)!!
                    val searchArtifact = findArtifact(artifactQualifiedName) as PsiNameIdentifierOwner
                    runReadAction {
                        val gutterMark = SourceMarkerUtils.getOrCreateMethodGutterMark(
                            fileMarker, searchArtifact.nameIdentifier!!
                        )!!
                        it.reply(gutterMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!)
                    }
                }
            }
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(FindAndOpenPortal) {
            val artifactQualifiedName = it.body()
            runReadAction {
                val sourceMarks = SourceMarker.getSourceMarks(artifactQualifiedName.identifier)
                if (sourceMarks.isNotEmpty()) {
                    val portal = sourceMarks[0].getUserData(SourceMarkKeys.SOURCE_PORTAL)!!
                    openPortal(portal)
                    it.reply(portal)
                }
            }
        }
        vertx.eventBus().consumer<SourcePortal>(OpenPortal) { openPortal(it.body()); it.reply(it.body()) }
        vertx.eventBus().consumer<SourcePortal>(ClosePortal) { closePortal(it.body()) }
        vertx.eventBus().consumer<SourcePortal>(RefreshOverview) {
            runReadAction {
                val fileMarker =
                    SourceMarker.getSourceFileMarker(getQualifiedClassName(it.body().viewingPortalArtifact)!!)!!
                GlobalScope.launch(vertx.dispatcher()) {
                    refreshOverview(fileMarker, it.body())
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
        vertx.eventBus().consumer<ArtifactQualifiedName>(CanNavigateToArtifact) {
            val artifactQualifiedName = it.body()
            val project = ProjectManager.getInstance().openProjects[0]
            GlobalScope.launch(vertx.dispatcher()) {
                it.reply(ArtifactNavigator.canNavigateTo(project, artifactQualifiedName))
            }
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(NavigateToArtifact) {
            val artifactQualifiedName = it.body()
            val project = ProjectManager.getInstance().openProjects[0]
            ArtifactNavigator.navigateTo(project, artifactQualifiedName)
        }
    }

    private suspend fun refreshTraces(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
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
                summarizedMetrics.add(ArtifactSummarizedMetrics(metricType, avg))
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
                vertx.eventBus().send(ArtifactMetricUpdated, metricResult.copy(artifactMetrics = finalArtifactMetrics))
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
                val port = vertx.sharedData().getLocalMap<String, Int>("portal")["http.port"]!!
                val host = "http://localhost:$port"
                val currentUrl = "$host/?portalUuid=${portal.portalUuid}"

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
