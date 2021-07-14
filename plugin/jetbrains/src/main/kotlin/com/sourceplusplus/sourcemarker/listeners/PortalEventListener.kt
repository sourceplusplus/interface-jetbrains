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
import com.sourceplusplus.marker.source.mark.api.ClassSourceMark
import com.sourceplusplus.marker.source.mark.api.MethodSourceMark
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.component.jcef.SourceMarkJcefComponent
import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.average
import com.sourceplusplus.monitor.skywalking.bridge.EndpointMetricsBridge
import com.sourceplusplus.monitor.skywalking.bridge.EndpointTracesBridge
import com.sourceplusplus.monitor.skywalking.bridge.LogsBridge
import com.sourceplusplus.monitor.skywalking.bridge.LogsBridge.GetEndpointLogs
import com.sourceplusplus.monitor.skywalking.model.GetEndpointMetrics
import com.sourceplusplus.monitor.skywalking.model.GetEndpointTraces
import com.sourceplusplus.monitor.skywalking.model.ZonedDuration
import com.sourceplusplus.monitor.skywalking.toProtocol
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactLogUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactMetricsUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactTracesUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.CanNavigateToArtifact
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedStackTraceElement
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClosePortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.FindAndOpenPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.FindPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.GetPortalConfiguration
import com.sourceplusplus.protocol.ProtocolAddress.Global.GetPortalTranslations
import com.sourceplusplus.protocol.ProtocolAddress.Global.NavigateToArtifact
import com.sourceplusplus.protocol.ProtocolAddress.Global.OpenPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.QueryTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshActivity
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshLogs
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshOverview
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetCurrentPage
import com.sourceplusplus.protocol.ProtocolAddress.Global.TraceSpanUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Portal.UpdateEndpoints
import com.sourceplusplus.protocol.SourceMarkerServices.Instance
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType
import com.sourceplusplus.protocol.artifact.endpoint.EndpointResult
import com.sourceplusplus.protocol.artifact.endpoint.EndpointType
import com.sourceplusplus.protocol.artifact.exception.JvmStackTraceElement
import com.sourceplusplus.protocol.artifact.metrics.ArtifactSummarizedMetrics
import com.sourceplusplus.protocol.artifact.metrics.ArtifactSummarizedResult
import com.sourceplusplus.protocol.artifact.metrics.MetricType
import com.sourceplusplus.protocol.artifact.trace.TraceResult
import com.sourceplusplus.protocol.artifact.trace.TraceSpan
import com.sourceplusplus.protocol.error.AccessDenied
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.protocol.utils.ArtifactNameUtils.getQualifiedClassName
import com.sourceplusplus.protocol.view.LiveViewConfig
import com.sourceplusplus.protocol.view.LiveViewSubscription
import com.sourceplusplus.sourcemarker.PluginBundle
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys
import com.sourceplusplus.sourcemarker.mark.SourceMarkKeys.ENDPOINT_DETECTOR
import com.sourceplusplus.sourcemarker.navigate.ArtifactNavigator
import com.sourceplusplus.sourcemarker.search.ArtifactSearch.findArtifact
import com.sourceplusplus.sourcemarker.search.SourceMarkSearch
import com.sourceplusplus.sourcemarker.settings.SourceMarkerConfig
import io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.ZonedDateTime
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
                PageType.LOGS -> vertx.eventBus().send(RefreshLogs, portal)
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
            log.info("Set portal ${portal.portalUuid} page type to $pageType")
            vertx.eventBus().publish(RefreshPortal, portal)
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
                val keys = PluginBundle.resourceBundle.keys
                while (keys.hasMoreElements()) {
                    val key = keys.nextElement()
                    map[key] = PluginBundle.resourceBundle.getString(key)
                }
                it.reply(JsonObject.mapFrom(map))
            }
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
            val portal = it.body()
            //pull from skywalking
            GlobalScope.launch(vertx.dispatcher()) {
                refreshActivity(portal)
            }

            //update subscriptions
            if (Instance.liveView != null) {
                Instance.liveView!!.clearLiveViewSubscriptions {
                    if (it.succeeded()) {
                        GlobalScope.launch(vertx.dispatcher()) {
                            val sourceMark = SourceMarker.getSourceMark(
                                portal.viewingPortalArtifact, SourceMark.Type.GUTTER
                            ) ?: return@launch
                            val endpointName = sourceMark.getUserData(
                                ENDPOINT_DETECTOR
                            )?.getOrFindEndpointName(sourceMark) ?: return@launch

                            Instance.liveView!!.addLiveViewSubscription(
                                LiveViewSubscription(
                                    null,
                                    listOf(endpointName),
                                    sourceMark.artifactQualifiedName,
                                    LiveViewConfig(
                                        "ACTIVITY",
                                        false,
                                        listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla"),
                                        0
                                    )
                                )
                            ) {
                                if (it.failed()) {
                                    log.error("Failed to add live view subscription", it.cause())
                                }
                            }
                        }
                    } else {
                        log.error("Failed to clear live view subscriptions", it.cause())
                    }
                }
            }
        }
        vertx.eventBus().consumer<SourcePortal>(RefreshTraces) {
            val portal = it.body()
            //pull from skywalking
            GlobalScope.launch(vertx.dispatcher()) {
                refreshTraces(it.body())
            }

            //update subscriptions
            if (Instance.liveView != null) {
                Instance.liveView!!.clearLiveViewSubscriptions {
                    if (it.succeeded()) {
                        GlobalScope.launch(vertx.dispatcher()) {
                            val sourceMark = SourceMarker.getSourceMark(
                                portal.viewingPortalArtifact, SourceMark.Type.GUTTER
                            ) ?: return@launch
                            val endpointName = sourceMark.getUserData(
                                ENDPOINT_DETECTOR
                            )?.getOrFindEndpointName(sourceMark) ?: return@launch

                            Instance.liveView!!.addLiveViewSubscription(
                                LiveViewSubscription(
                                    null,
                                    listOf(endpointName),
                                    sourceMark.artifactQualifiedName,
                                    LiveViewConfig(
                                        "TRACES",
                                        false,
                                        listOf("endpoint_traces"),
                                        0
                                    )
                                )
                            ) {
                                if (it.failed()) {
                                    log.error("Failed to add live view subscription", it.cause())
                                }
                            }
                        }
                    } else {
                        log.error("Failed to clear live view subscriptions", it.cause())
                    }
                }
            }
        }
        vertx.eventBus().consumer<SourcePortal>(RefreshLogs) {
            val portal = it.body()
            //pull from skywalking
            GlobalScope.launch(vertx.dispatcher()) {
                refreshLogs(portal)
            }

            //update subscriptions
            if (Instance.liveView != null) {
                Instance.liveView!!.clearLiveViewSubscriptions {
                    if (it.succeeded()) {
                        GlobalScope.launch(vertx.dispatcher()) {
                            val sourceMark = SourceMarker.getSourceMark(
                                portal.viewingPortalArtifact, SourceMark.Type.GUTTER
                            ) as MethodSourceMark? ?: return@launch
                            val logPatterns = sourceMark.getUserData(SourceMarkKeys.LOGGER_DETECTOR)!!
                                .getOrFindLoggerStatements(sourceMark).map { it.logPattern }

                            Instance.liveView!!.addLiveViewSubscription(
                                LiveViewSubscription(
                                    null,
                                    logPatterns,
                                    sourceMark.artifactQualifiedName,
                                    LiveViewConfig(
                                        "LOGS",
                                        false,
                                        listOf("endpoint_logs"),
                                        0
                                    )
                                )
                            ) {
                                if (it.failed()) {
                                    log.error("Failed to add live view subscription", it.cause())
                                }
                            }
                        }
                    } else {
                        log.error("Failed to clear live view subscriptions", it.cause())
                    }
                }
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
            log.info("Clicked stack trace element: $element")

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
                                ZonedDateTime.now().minusHours(24),
                                ZonedDateTime.now(),
                                SkywalkingClient.DurationStep.MINUTE
                            ),
                            orderType = portal.tracesView.orderType,
                            pageSize = portal.tracesView.viewTraceAmount,
                            pageNumber = portal.tracesView.pageNumber
                        ), vertx
                    )

                    handleTraceResult(traceResult, portal, portal.viewingPortalArtifact)
                }
            } else if (Instance.localTracing != null) {
                portal.tracesView.localTracing = true
                Instance.localTracing!!.getTraceResult(
                    artifactQualifiedName = ArtifactQualifiedName(
                        identifier = portal.viewingPortalArtifact,
                        commitId = "null",
                        type = ArtifactType.METHOD
                    ),
                    start = ZonedDateTime.now().minusHours(24).toInstant().toKotlinInstant(),
                    stop = ZonedDateTime.now().toInstant().toKotlinInstant(),
                    orderType = portal.tracesView.orderType,
                    pageSize = portal.tracesView.viewTraceAmount,
                    pageNumber = portal.tracesView.pageNumber,
                ) {
                    if (it.succeeded()) {
                        handleTraceResult(it.result(), portal, portal.viewingPortalArtifact)
                    } else {
                        val replyException = it.cause() as ReplyException
                        if (replyException.failureType() == ReplyFailure.TIMEOUT) {
                            log.warn("Timed out getting local trace results")
                        } else {
                            val actualException = replyException.cause!!
                            if (actualException is AccessDenied) {
                                log.error("Access denied. Reason: " + actualException.reason)
                            } else {
                                it.cause().printStackTrace()
                                log.error("Failed to get local trace results", it.cause())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleTraceResult(traceResult: TraceResult, portal: SourcePortal, artifactQualifiedName: String) {
        //todo: rename {GET} to [GET] in skywalking
        if (markerConfig.autoResolveEndpointNames) {
            GlobalScope.launch(vertx.dispatcher()) {
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
                                val updatedEndpointName = "{$httpMethod}${URI(url).path}"
                                vertx.eventBus().send(
                                    TraceSpanUpdated, entrySpan.copy(
                                        endpointName = updatedEndpointName,
                                        artifactQualifiedName = artifactQualifiedName
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        vertx.eventBus().send(ArtifactTracesUpdated, traceResult)
    }

    private suspend fun refreshLogs(portal: SourcePortal) {
        if (log.isTraceEnabled) log.trace("Refreshing logs. Portal: {}", portal.portalUuid)
        val sourceMark = SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
        GlobalScope.launch(vertx.dispatcher()) {
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
    }

    private suspend fun refreshOverview(fileMarker: SourceFileMarker, portal: SourcePortal) {
        val endpointMarks = fileMarker.getSourceMarks().filterIsInstance<MethodSourceMark>().filter {
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
        val sourceMark = SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null && sourceMark is MethodSourceMark) {
            val endpointId = sourceMark.getUserData(ENDPOINT_DETECTOR)!!.getOrFindEndpointId(sourceMark)
            if (endpointId != null) {
                val endTime = ZonedDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
                val startTime = endTime.minusMinutes(portal.activityView.timeFrame.minutes.toLong())
                val metricsRequest = GetEndpointMetrics(
                    listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla"),
                    endpointId,
                    ZonedDuration(startTime, endTime, SkywalkingClient.DurationStep.MINUTE)
                )
                val metrics = EndpointMetricsBridge.getMetrics(metricsRequest, vertx)
                val metricResult = toProtocol(
                    portal.appUuid,
                    portal.viewingPortalArtifact,
                    portal.activityView.timeFrame,
                    portal.activityView.activeChartMetric,
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
//                val multiMetrics = EndpointMetricsBridge.getMultipleMetrics(multipleMetricsRequest, vertx)
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

                vertx.eventBus().send(ArtifactMetricsUpdated, metricResult.copy(artifactMetrics = finalArtifactMetrics))
            }
        }
    }

    private fun openPortal(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null) {
            val jcefComponent = sourceMark.sourceMarkComponent as SourceMarkJcefComponent
            if (portal != lastDisplayedInternalPortal) {
                val externalEndpoint = sourceMark.getUserData(ENDPOINT_DETECTOR)?.isExternalEndpoint(sourceMark) == true
                if (externalEndpoint) {
                    portal.configuration.visibleActivity = true
                    portal.configuration.visibleTraces = true
                    portal.configuration.visibleLogs = true //todo: can hide based on if there is logs
                } else if (Instance.localTracing != null) {
                    portal.configuration.visibleTraces = true
                } else {
                    //non-endpoint artifact; hide activity/traces till manually shown
                    portal.configuration.visibleActivity = false
                    portal.configuration.visibleTraces = portal.tracesView.innerTraceStack

                    //default to logs if method
                    if (sourceMark is MethodSourceMark && !portal.configuration.visibleTraces) {
                        portal.configuration.currentPage = PageType.LOGS
                    }

                    //hide overview if class and no child endpoints and default to logs
                    if (sourceMark is ClassSourceMark) {
                        val hasChildEndpoints = sourceMark.sourceFileMarker.getSourceMarks().firstOrNull {
                            it.getUserData(ENDPOINT_DETECTOR)?.getEndpointId(it) != null
                        } != null
                        portal.configuration.visibleOverview = hasChildEndpoints
                        if (!hasChildEndpoints) {
                            portal.configuration.currentPage = PageType.LOGS
                        }
                    }
                }

                val lastViewedPage = lastDisplayedInternalPortal?.configuration?.currentPage
                if (lastViewedPage != null && portal.configuration.isViewable(lastViewedPage)) {
                    portal.configuration.currentPage = lastViewedPage
                }
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

            ApplicationManager.getApplication().invokeLater(sourceMark::displayPopup)
        }
    }

    private fun closePortal(portal: SourcePortal) {
        val sourceMark = SourceMarker.getSourceMark(portal.viewingPortalArtifact, SourceMark.Type.GUTTER)
        if (sourceMark != null) {
            ApplicationManager.getApplication().invokeLater(sourceMark::closePopup)
        }
    }
}
