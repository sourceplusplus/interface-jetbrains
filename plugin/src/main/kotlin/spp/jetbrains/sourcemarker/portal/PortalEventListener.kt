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
package spp.jetbrains.sourcemarker.portal

import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.ProjectManager
import io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.slf4j.LoggerFactory
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.ArtifactNavigator
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ClassSourceMark
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.component.jcef.SourceMarkJcefComponent
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventCode
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.average
import spp.jetbrains.monitor.skywalking.bridge.EndpointMetricsBridge
import spp.jetbrains.monitor.skywalking.bridge.EndpointTracesBridge
import spp.jetbrains.monitor.skywalking.bridge.LogsBridge
import spp.jetbrains.monitor.skywalking.bridge.LogsBridge.GetEndpointLogs
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.jetbrains.monitor.skywalking.toProtocol
import spp.jetbrains.portal.SourcePortal
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.ArtifactLogUpdated
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.ArtifactMetricsUpdated
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.ArtifactTracesUpdated
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.CanNavigateToArtifact
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.ClickedStackTraceElement
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.ClosePortal
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.FindAndOpenPortal
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.FindPortal
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.GetPortalConfiguration
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.GetPortalTranslations
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.NavigateToArtifact
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.OpenPortal
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.QueryTraceStack
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.RefreshActivity
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.RefreshLogs
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.RefreshOverview
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.RefreshPortal
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.RefreshTraces
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.SetCurrentPage
import spp.jetbrains.portal.protocol.ProtocolAddress.Global.TraceSpanUpdated
import spp.jetbrains.portal.protocol.ProtocolAddress.Portal.UpdateEndpoints
import spp.jetbrains.portal.protocol.artifact.endpoint.EndpointResult
import spp.jetbrains.portal.protocol.artifact.endpoint.EndpointType
import spp.jetbrains.portal.protocol.artifact.metrics.ArtifactSummarizedMetrics
import spp.jetbrains.portal.protocol.artifact.metrics.ArtifactSummarizedResult
import spp.jetbrains.portal.protocol.portal.PageType
import spp.jetbrains.sourcemarker.PluginBundle
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import spp.jetbrains.sourcemarker.mark.SourceMarkSearch
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.SourceServices.Instance
import spp.protocol.SourceServices.Provide.toLiveViewSubscriberAddress
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.QueryTimeFrame
import spp.protocol.artifact.exception.LiveStackTraceElement
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
import spp.protocol.instrument.LiveSourceLocation
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import spp.protocol.view.LiveViewSubscription
import java.net.URI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit
import javax.swing.UIManager

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class PortalEventListener(
    private val markerConfig: SourceMarkerConfig,
    private val hostTranslations: Boolean = true
) : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(PortalEventListener::class.java)
    }

    private val formatter = DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmm")
        .toFormatter()
        .withZone(ZoneOffset.UTC) //todo: load from SkywalkingMonitor
    private var lastDisplayedInternalPortal: SourcePortal? = null

    override suspend fun start() {
        //listen for theme changes
        UIManager.addPropertyChangeListener {
            if (lastDisplayedInternalPortal != null) {
                lastDisplayedInternalPortal!!.configuration.darkMode = (it.newValue !is IntelliJLaf)
                val sourceMark = SourceMarker.getSourceMark(
                    lastDisplayedInternalPortal!!.viewingArtifact, SourceMark.Type.GUTTER
                )
                if (sourceMark != null) {
                    val jcefComponent = sourceMark.sourceMarkComponent as SourceMarkJcefComponent
                    jcefComponent.getBrowser().cefBrowser.reload()
                }
            }
        }

        //listen to live view events
        var developer = "system"
        if (markerConfig.serviceToken != null) {
            val json = JWT.parse(markerConfig.serviceToken)
            developer = json.getJsonObject("payload").getString("developer_id")
        }
        vertx.eventBus().consumer<JsonObject>(toLiveViewSubscriberAddress(developer)) {
            val event = Json.decodeValue(it.body().toString(), LiveViewEvent::class.java)
            when (event.viewConfig.viewName) {
                "LOGS" -> launch(vertx.dispatcher()) { consumeLogsViewEvent(event) }
                "TRACES" -> consumeTracesViewEvent(event)
                "ACTIVITY" -> consumeActivityViewEvent(event)
            }
        }

        vertx.eventBus().consumer<Any>(RefreshPortal) {
            val portal = if (it.body() is String) {
                SourcePortal.getPortal(it.body() as String)
            } else {
                it.body() as SourcePortal
            }!!
            when (portal.configuration.config["currentPage"]) {
                PageType.OVERVIEW -> vertx.eventBus().send(RefreshOverview, portal)
                PageType.ACTIVITY -> vertx.eventBus().send(RefreshActivity, portal)
                PageType.LOGS -> vertx.eventBus().send(RefreshLogs, portal)
                PageType.TRACES -> vertx.eventBus().send(RefreshTraces, portal)
                PageType.CONFIGURATION -> TODO()
            }
        }
        vertx.eventBus().consumer<Any>(SetCurrentPage) {
            if (it.body() is JsonObject) {
                val body = (it.body() as JsonObject)
                val portalUuid = body.getString("portalUuid")
                val pageType = PageType.valueOf(body.getString("pageType"))
                val portal = SourcePortal.getPortal(portalUuid)!!
                portal.configuration.config["currentPage"] = pageType
                it.reply(JsonObject.mapFrom(portal.configuration))
                log.info("Set portal ${portal.portalUuid} page type to $pageType")
                vertx.eventBus().publish(RefreshPortal, portal)
            } else {
                val portal = it.body() as SourcePortal
                if (lastDisplayedInternalPortal == null) {
                    configureDisplayedPortal(portal)
                } else {
                    val sourceMark = SourceMarker.getSourceMark(portal.viewingArtifact, SourceMark.Type.GUTTER)
                    val jcefComponent = sourceMark!!.sourceMarkComponent as SourceMarkJcefComponent
                    val port = vertx.sharedData().getLocalMap<String, Int>("portal")["http.port"]!!
                    val host = "http://localhost:$port"
                    val currentUrl = "$host/?portalUuid=${portal.portalUuid}"
                    jcefComponent.getBrowser().cefBrowser.executeJavaScript(
                        "window.location.href = '$currentUrl';", currentUrl, 0
                    )
                }
                it.reply(JsonObject.mapFrom(portal.configuration))
                log.info("Updated portal ${portal.portalUuid} current page")
            }
        }
        vertx.eventBus().consumer<String>(GetPortalConfiguration) {
            val portalUuid = it.body()
            if (!portalUuid.isNullOrEmpty()) {
                log.info("Getting portal configuration. Portal UUID: $portalUuid")
                val portal = SourcePortal.getPortal(portalUuid)
                if (portal == null) {
                    log.error("Failed to find portal: $portalUuid")
                    it.fail(NOT_FOUND.code(), "Portal $portalUuid does not exist")
                } else {
                    it.reply(JsonObject.mapFrom(portal.configuration))
                }
            } else {
                log.error("Failed to get portal configuration. Missing portalUuid");
            }
        }
        if (hostTranslations) {
            vertx.eventBus().consumer<String>(GetPortalTranslations) {
                val map = HashMap<String, String>()
                val keys = PluginBundle.LOCALE_BUNDLE.keys
                while (keys.hasMoreElements()) {
                    val key = keys.nextElement()
                    map[key] = PluginBundle.LOCALE_BUNDLE.getString(key)
                }
                it.reply(JsonObject.mapFrom(map))
            }
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(FindPortal) {
//            val artifactQualifiedName = it.body()
//            val portals = SourcePortal.getPortals(artifactQualifiedName)
//            if (portals.isNotEmpty()) {
//                it.reply(portals.first())
//            } else {
//                launch(vertx.dispatcher()) {
//                    val classArtifact = findArtifact(
//                        vertx, artifactQualifiedName.copy(
//                            identifier = ArtifactNameUtils.getQualifiedClassName(artifactQualifiedName.identifier)!!,
//                            operationName = null,
//                            type = ArtifactType.CLASS
//                        )
//                    )
//                    val fileMarker = SourceMarker.getSourceFileMarker(classArtifact!!.containingFile)!!
//                    val searchArtifact = findArtifact(vertx, artifactQualifiedName) as PsiNameIdentifierOwner
//                    runReadAction {
//                        val gutterMark = creationService.getOrCreateMethodGutterMark(
//                            fileMarker, searchArtifact.nameIdentifier!!
//                        )!!
//                        println(gutterMark)
//                        //it.reply(gutterMark.getUserData(SourceMarkKeys.SOURCE_PORTAL)!!)
//                    }
//                }
//            }
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(FindAndOpenPortal) {
//            val artifactQualifiedName = it.body()
//            runReadAction {
//                val sourceMarks = SourceMarker.getSourceMarks(artifactQualifiedName)
//                if (sourceMarks.isNotEmpty()) {
//                    val sourceMark = sourceMarks[0]
//                    ApplicationManager.getApplication().invokeLater {
//                        PsiNavigateUtil.navigate(sourceMark.getPsiElement())
//
//                        val portals = SourcePortal.getPortals(artifactQualifiedName)
//                        openPortal(portals.first())
//                        it.reply(portals.first())
//                    }
//                } else {
//                    log.warn("Failed to find portal for artifact: $artifactQualifiedName")
//                }
//            }
        }
        vertx.eventBus().consumer<SourcePortal>(OpenPortal) { openPortal(it.body()); it.reply(it.body()) }
        vertx.eventBus().consumer<SourcePortal>(ClosePortal) { closePortal(it.body()) }
        vertx.eventBus().consumer<SourcePortal>(RefreshOverview) {
            runReadAction {
                val fileMarker = SourceMarker.getSourceFileMarker(it.body().viewingArtifact)!!
                launch(vertx.dispatcher()) {
                    refreshOverview(fileMarker, it.body())
                }

                //todo: update subscriptions
            }
        }
        vertx.eventBus().consumer<SourcePortal>(RefreshActivity) {
            val portal = it.body()
            //pull from skywalking
            launch(vertx.dispatcher()) {
                pullLatestActivity(portal)
            }

            //update subscriptions
            launch(vertx.dispatcher()) {
                val sourceMark = SourceMarker.getSourceMark(
                    portal.viewingArtifact, SourceMark.Type.GUTTER
                ) ?: return@launch
                val endpointName = sourceMark.getUserData(
                    ENDPOINT_DETECTOR
                )?.getOrFindEndpointName(sourceMark) ?: return@launch
                val endpointId = sourceMark.getUserData(
                    ENDPOINT_DETECTOR
                )?.getOrFindEndpointId(sourceMark) ?: return@launch

                Instance.liveView!!.addLiveViewSubscription(
                    LiveViewSubscription(
                        null,
                        listOf(endpointName),
                        portal.viewingArtifact.copy(operationName = endpointId), //todo: only SWLiveViewService uses
                        LiveSourceLocation(portal.viewingArtifact.identifier, 0), //todo: fix
                        LiveViewConfig("ACTIVITY", listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla"))
                    )
                ).onComplete {
                    if (it.succeeded()) {
                        val subscriptionId = it.result().subscriptionId!!
                        if (portal.configuration.config["subscriptionId"] != null) {
                            Instance.liveView!!.removeLiveViewSubscription(
                                portal.configuration.config["subscriptionId"].toString()
                            )
                        }
                        portal.configuration.config["subscriptionId"] = subscriptionId

                        sourceMark.addEventListener {
                            if (it.eventCode == SourceMarkEventCode.PORTAL_CLOSED) {
                                Instance.liveView!!.removeLiveViewSubscription(subscriptionId)
                            }
                        }
                    } else {
                        log.error("Failed to add live view subscription", it.cause())
                    }
                }
            }
        }
        vertx.eventBus().consumer<SourcePortal>(RefreshTraces) {
            val portal = it.body()
            //pull from skywalking
            launch(vertx.dispatcher()) {
                pullLatestTraces(it.body())
            }

            //update subscriptions
            launch(vertx.dispatcher()) {
                val sourceMark = SourceMarker.getSourceMark(
                    portal.viewingArtifact, SourceMark.Type.GUTTER
                ) ?: return@launch
                val endpointName = sourceMark.getUserData(
                    ENDPOINT_DETECTOR
                )?.getOrFindEndpointName(sourceMark) ?: return@launch
                val endpointId = sourceMark.getUserData(
                    ENDPOINT_DETECTOR
                )?.getOrFindEndpointId(sourceMark) ?: return@launch

                Instance.liveView!!.addLiveViewSubscription(
                    LiveViewSubscription(
                        null,
                        listOf(endpointName),
                        portal.viewingArtifact.copy(operationName = endpointId), //todo: only SWLiveViewService uses
                        LiveSourceLocation(portal.viewingArtifact.identifier, 0), //todo: fix
                        LiveViewConfig("TRACES", listOf("endpoint_traces"))
                    )
                ).onComplete {
                    if (it.succeeded()) {
                        val subscriptionId = it.result().subscriptionId!!
                        if (portal.configuration.config["subscriptionId"] != null) {
                            Instance.liveView!!.removeLiveViewSubscription(
                                portal.configuration.config["subscriptionId"].toString()
                            )
                        }
                        portal.configuration.config["subscriptionId"] = subscriptionId

                        sourceMark.addEventListener {
                            if (it.eventCode == SourceMarkEventCode.PORTAL_CLOSED) {
                                Instance.liveView!!.removeLiveViewSubscription(subscriptionId)
                            }
                        }
                    } else {
                        log.error("Failed to add live view subscription", it.cause())
                    }
                }
            }
        }
        vertx.eventBus().consumer<SourcePortal>(RefreshLogs) {
            val portal = it.body()
            //pull from skywalking
            launch(vertx.dispatcher()) {
                pullLatestLogs(portal)
            }

            //update subscriptions
            launch(vertx.dispatcher()) {
                val sourceMark = SourceMarker.getSourceMark(
                    portal.viewingArtifact, SourceMark.Type.GUTTER
                ) ?: return@launch
                val logPatterns = if (sourceMark is ClassSourceMark) {
                    sourceMark.sourceFileMarker.getSourceMarks().filterIsInstance<MethodSourceMark>()
                        .flatMap {
                            it.getUserData(SourceMarkKeys.LOGGER_DETECTOR)!!
                                .getOrFindLoggerStatements(it)
                        }.map { it.logPattern }
                } else if (sourceMark is MethodSourceMark) {
                    sourceMark.getUserData(SourceMarkKeys.LOGGER_DETECTOR)!!
                        .getOrFindLoggerStatements(sourceMark).map { it.logPattern }
                } else {
                    throw IllegalStateException("Unsupported source mark type")
                }

                Instance.liveView!!.addLiveViewSubscription(
                    LiveViewSubscription(
                        null,
                        logPatterns,
                        portal.viewingArtifact,
                        LiveSourceLocation(portal.viewingArtifact.identifier, 0), //todo: fix
                        LiveViewConfig("LOGS", listOf("endpoint_logs"))
                    )
                ).onComplete {
                    if (it.succeeded()) {
                        val subscriptionId = it.result().subscriptionId!!
                        if (portal.configuration.config["subscriptionId"] != null) {
                            Instance.liveView!!.removeLiveViewSubscription(
                                portal.configuration.config["subscriptionId"].toString()
                            )
                        }
                        portal.configuration.config["subscriptionId"] = subscriptionId

                        sourceMark.addEventListener {
                            if (it.eventCode == SourceMarkEventCode.PORTAL_CLOSED) {
                                Instance.liveView!!.removeLiveViewSubscription(subscriptionId)
                            }
                        }
                    } else {
                        log.error("Failed to add live view subscription", it.cause())
                    }
                }
            }
        }
        vertx.eventBus().consumer<String>(QueryTraceStack) { handler ->
            val traceId = handler.body()
            launch(vertx.dispatcher()) {
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
                LiveStackTraceElement::class.java
            )
            log.info("Clicked stack trace element: $element")

            val project = ProjectManager.getInstance().openProjects[0]
            ArtifactNavigator.navigateTo(project, element)
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(CanNavigateToArtifact) {
            val artifactQualifiedName = it.body()
            val project = ProjectManager.getInstance().openProjects[0]
            launch(vertx.dispatcher()) {
                it.reply(ArtifactNavigator.canNavigateTo(project, artifactQualifiedName))
            }
        }
        vertx.eventBus().consumer<ArtifactQualifiedName>(NavigateToArtifact) { msg ->
            launch(vertx.dispatcher()) {
                ArtifactNavigator.navigateTo(vertx, msg.body()) {
                    if (it.succeeded()) {
                        log.info("Navigated to artifact $it")
                        msg.reply(it.result())
                    } else {
                        log.error("Failed to navigate to artifact", it.cause())
                        msg.fail(500, it.cause().message)
                    }
                }
            }
        }
    }

    private suspend fun pullLatestTraces(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null && sourceMark is MethodSourceMark) {
            val endpointId = sourceMark.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(sourceMark)
            if (endpointId != null) {
                val traceResult = EndpointTracesBridge.getTraces(
                    GetEndpointTraces(
                        artifactQualifiedName = portal.viewingArtifact,
                        endpointId = endpointId,
                        zonedDuration = ZonedDuration(
                            ZonedDateTime.now().minusHours(24),
                            ZonedDateTime.now(),
                            SkywalkingClient.DurationStep.MINUTE
                        ),
                        orderType = portal.tracesView.orderType,
                        pageSize = portal.tracesView.viewTraceAmount,
                        pageNumber = portal.tracesView.pageNumber
                    ), vertx
                )
                vertx.eventBus().send(ArtifactTracesUpdated, traceResult)

                if (markerConfig.autoResolveEndpointNames) {
                    autoResolveEndpointNames(traceResult, portal)
                }
            }
        }
    }

    private suspend fun autoResolveEndpointNames(traceResult: TraceResult, portal: SourcePortal) {
        //todo: only try to auto resolve endpoint names with dynamic ids
        //todo: support multiple operationsNames/traceIds
        traceResult.traces.forEach {
            if (!portal.tracesView.resolvedEndpointNames.containsKey(it.traceIds[0])) {
                val traceStack = EndpointTracesBridge.getTraceStack(it.traceIds[0], vertx)
                val entrySpan: TraceSpan? = traceStack.traceSpans.firstOrNull { it.type == "Entry" }
                if (entrySpan != null) {
                    val url = entrySpan.tags["url"]
                    val httpMethod = entrySpan.tags["http.method"]
                    if (url != null && httpMethod != null) {
                        val updatedEndpointName = "$httpMethod:${URI(url).path}"
                        vertx.eventBus().send(
                            TraceSpanUpdated, entrySpan.copy(
                                endpointName = updatedEndpointName,
                                artifactQualifiedName = portal.viewingArtifact
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun pullLatestLogs(portal: SourcePortal) {
        if (log.isTraceEnabled) log.trace("Refreshing logs. Portal: {}", portal.portalUuid)
        val sourceMark = SourceMarker.getSourceMark(portal.viewingArtifact, SourceMark.Type.GUTTER)
        val logsResult = LogsBridge.queryLogs(
            GetEndpointLogs(
                endpointId = if (sourceMark is MethodSourceMark) {
                    sourceMark.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(sourceMark)
                } else null,
                zonedDuration = ZonedDuration(
                    ZonedDateTime.now().minusMinutes(15), //todo: method filtering in skywalking
                    ZonedDateTime.now(),
                    SkywalkingClient.DurationStep.MINUTE
                ),
                orderType = portal.logsView.orderType,
                pageSize = portal.logsView.viewLogAmount * 25, //todo: method filtering in skywalking
                pageNumber = portal.logsView.pageNumber
            ), vertx
        )
        if (logsResult.succeeded()) {
            //todo: impl method filtering in skywalking
            for ((content, logs) in logsResult.result().logs.groupBy { it.content }) {
                SourceMarkSearch.findInheritedSourceMarks(content).forEach {
                    vertx.eventBus().send(
                        ArtifactLogUpdated, logsResult.result().copy(
                            artifactQualifiedName = it.artifactQualifiedName,
                            total = logs.size,
                            logs = logs,
                        )
                    )
                }
            }
        } else {
            val replyException = logsResult.cause() as ReplyException
            if (replyException.failureCode() == 404) {
                log.warn("Failed to fetch logs. Service(s) unavailable")
            } else {
                log.error("Failed to fetch logs", logsResult.cause())
            }
        }
    }

    private suspend fun refreshOverview(fileMarker: SourceFileMarker, portal: SourcePortal) {
        val endpointMarks = fileMarker.getSourceMarks().filterIsInstance<MethodGutterMark>().filter {
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
                    it.artifactQualifiedName.copy(operationName = endpointName),
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
                        portal.overviewView.timeFrame,
                        start = Instant.fromEpochMilliseconds(requestDuration.start.toInstant().toEpochMilli()),
                        stop = Instant.fromEpochMilliseconds(requestDuration.stop.toInstant().toEpochMilli()),
                        step = requestDuration.step.name,
                        endpointMetricResults
                    )
                )
            )
        )
    }

    private suspend fun pullLatestActivity(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null && sourceMark is MethodSourceMark) {
            val endpointId = sourceMark.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(sourceMark)
            if (endpointId != null) {
                pullLatestActivity(portal, endpointId)
            }
        }
    }

    private suspend fun pullLatestActivity(portal: SourcePortal, endpointId: String) {
        val endTime = ZonedDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
        val startTime = endTime.minusMinutes(portal.activityView.timeFrame.minutes.toLong())
        val metricsRequest = GetEndpointMetrics(
            listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla"),
            endpointId,
            ZonedDuration(startTime, endTime, SkywalkingClient.DurationStep.MINUTE)
        )
        val metrics = EndpointMetricsBridge.getMetrics(metricsRequest, vertx)
        val metricResult = toProtocol(
            portal.viewingArtifact,
            portal.activityView.timeFrame,
            portal.activityView.activeChartMetric,
            metricsRequest,
            metrics
        )

        val finalArtifactMetrics = metricResult.artifactMetrics.toMutableList()
        vertx.eventBus().send(ArtifactMetricsUpdated, metricResult.copy(artifactMetrics = finalArtifactMetrics))
    }

    private fun openPortal(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null) {
            configureDisplayedPortal(portal)
            ApplicationManager.getApplication().invokeLater(sourceMark::displayPopup)
        }
    }

    private fun configureDisplayedPortal(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null) {
            val jcefComponent = sourceMark.sourceMarkComponent as SourceMarkJcefComponent
            if (portal != lastDisplayedInternalPortal) {
                portal.configuration.darkMode = UIManager.getLookAndFeel() !is IntelliJLaf

                val externalEndpoint = sourceMark.getUserData(ENDPOINT_DETECTOR)?.isExternalEndpoint(sourceMark) == true
                if (externalEndpoint) {
                    portal.configuration.config["visibleActivity"] = true
                    portal.configuration.config["visibleTraces"] = true
                    portal.configuration.config["visibleLogs"] = true //todo: can hide based on if there is logs
                } else {
                    //non-endpoint artifact; hide activity/traces till manually shown
                    portal.configuration.config["visibleActivity"] = false
                    portal.configuration.config["visibleTraces"] = portal.tracesView.innerTraceStack

                    //default to logs if method
                    if (sourceMark is MethodSourceMark && !(portal.configuration.config["visibleTraces"] as Boolean)) {
                        portal.configuration.config["currentPage"] = PageType.LOGS
                    }

                    //hide overview if class and no child endpoints and default to logs
                    if (sourceMark is ClassSourceMark) {
                        val hasChildEndpoints = sourceMark.sourceFileMarker.getSourceMarks().firstOrNull {
                            it.getUserData(ENDPOINT_DETECTOR)?.getEndpointId(it) != null
                        } != null
                        portal.configuration.config["visibleOverview"] = hasChildEndpoints
                        if (!hasChildEndpoints) {
                            portal.configuration.config["currentPage"] = PageType.LOGS
                        }
                    }
                }

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

    private fun consumeTracesViewEvent(event: LiveViewEvent) {
        val portal = SourcePortal.getPortals().find {
            it.configuration.config["subscriptionId"] == event.subscriptionId
        } ?: return

        val rawMetrics = JsonObject(event.metricsData)
        val trace = Json.decodeValue(rawMetrics.getJsonObject("trace").toString(), Trace::class.java)
        val traceResult = TraceResult(
            portal.viewingArtifact,
            null,
            TraceOrderType.LATEST_TRACES,
            trace.start,
            trace.start.toJavaInstant().minusMillis(trace.duration.toLong()).toKotlinInstant(),
            "minute",
            listOf(trace),
            Int.MAX_VALUE
        )
        vertx.eventBus().send(ArtifactTracesUpdated, traceResult)

        if (markerConfig.autoResolveEndpointNames) {
            //S++ adds trace meta to avoid additional query for auto-resolve endpoints
            val url = trace.meta["url"]
            val httpMethod = trace.meta["http.method"]
            val entrySpanJson = trace.meta["entrySpan"]
            if (url != null && httpMethod != null && entrySpanJson != null) {
                val updatedEndpointName = "$httpMethod:${URI(url).path}"
                val entrySpan = Json.decodeValue(entrySpanJson, TraceSpan::class.java)
                vertx.eventBus().send(
                    TraceSpanUpdated, entrySpan.copy(
                        endpointName = updatedEndpointName,
                        artifactQualifiedName = event.artifactQualifiedName
                    )
                )
            } else {
                launch(vertx.dispatcher()) {
                    autoResolveEndpointNames(traceResult, portal)
                }
            }
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
        for ((content, logs) in logsResult.logs.groupBy { it.content }) {
            SourceMarkSearch.findInheritedSourceMarks(content).forEach {
                vertx.eventBus().send(
                    ArtifactLogUpdated, logsResult.copy(
                        artifactQualifiedName = it.artifactQualifiedName,
                        total = logs.size,
                        logs = logs,
                    )
                )
            }
        }
    }

    private fun consumeActivityViewEvent(event: LiveViewEvent) {
        val portal = SourcePortal.getPortals().find {
            it.configuration.config["subscriptionId"] == event.subscriptionId
        } ?: return

        val artifactMetrics = toArtifactMetrics(event)
        val metricResult = ArtifactMetricResult(
            portal.viewingArtifact,
            QueryTimeFrame.valueOf(1),
            portal.activityView.activeChartMetric, //todo: assumes activity view
            formatter.parse(event.timeBucket, java.time.Instant::from).toKotlinInstant(),
            formatter.parse(event.timeBucket, java.time.Instant::from).plusSeconds(60).toKotlinInstant(),
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

    private fun closePortal(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null) {
            ApplicationManager.getApplication().invokeLater(sourceMark::closePopup)
        }
    }
}
