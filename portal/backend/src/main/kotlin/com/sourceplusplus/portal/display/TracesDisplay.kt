package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.extensions.displaySpanInfo
import com.sourceplusplus.portal.extensions.displayTraceStack
import com.sourceplusplus.portal.extensions.displayTraces
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.display.views.TracesView
import com.sourceplusplus.protocol.ArtifactNameUtils.getShortQualifiedFunctionName
import com.sourceplusplus.protocol.ArtifactNameUtils.removePackageAndClassName
import com.sourceplusplus.protocol.ArtifactNameUtils.removePackageNames
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ArtifactTraceUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.ClickedDisplayTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.GetTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.QueryTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.RefreshTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.Companion.TracesTabOpened
import com.sourceplusplus.protocol.artifact.trace.*
import com.sourceplusplus.protocol.portal.PageType
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.regex.Pattern

/**
 * Displays traces (and the underlying spans) for a given source code artifact.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TracesDisplay : AbstractDisplay(PageType.TRACES) {

    companion object {
        private val log = LoggerFactory.getLogger(TracesDisplay::class.java)
        val QUALIFIED_NAME_PATTERN = Pattern.compile(".+\\..+\\(.*\\)")!!
    }

    override suspend fun start() {
        super.start()

        vertx.setPeriodic(5000) {
            SourcePortal.getPortals().forEach {
                if (it.currentTab == PageType.TRACES) {
                    //todo: only update if external or internal and currently displaying
                    vertx.eventBus().send(RefreshTraces, it)
                }
            }
        }
        vertx.eventBus().consumer(TracesTabOpened, this@TracesDisplay::tracesTabOpened)
        vertx.eventBus().consumer<TraceResult>(ArtifactTraceUpdated) { handleArtifactTraceResult(it.body()) }
        vertx.eventBus().consumer(ClickedDisplayTraceStack, this@TracesDisplay::clickedDisplayTraceStack)
        vertx.eventBus().consumer(ClickedDisplayTraces, this@TracesDisplay::clickedDisplayTraces)
        vertx.eventBus().consumer(ClickedDisplaySpanInfo, this@TracesDisplay::clickedDisplaySpanInfo)
        vertx.eventBus().consumer(GetTraceStack, this@TracesDisplay::getTraceStack)
        log.info("{} started", javaClass.simpleName)
    }

    override fun updateUI(portal: SourcePortal) {
        if (portal.currentTab != thisTab) {
            return
        }

        when (portal.tracesView.viewType) {
            TracesView.Companion.ViewType.TRACES -> displayTraces(portal)
            TracesView.Companion.ViewType.TRACE_STACK -> displayTraceStack(portal)
            TracesView.Companion.ViewType.SPAN_INFO -> displaySpanInfo(portal)
        }
    }

    private fun clickedDisplaySpanInfo(messageHandler: Message<JsonObject>) {
        val spanInfoRequest = messageHandler.body() as JsonObject
        log.debug("Clicked display span info: {}", spanInfoRequest)

        val portalUuid = spanInfoRequest.getString("portal_uuid")
        val portal = SourcePortal.getPortal(portalUuid)!!
        val representation = portal.tracesView
        representation.viewType = TracesView.Companion.ViewType.SPAN_INFO
        representation.traceId = spanInfoRequest.getString("trace_id")
        representation.spanId = spanInfoRequest.getInteger("span_id")
        updateUI(portal)
    }

    private fun clickedDisplayTraces(it: Message<JsonObject>) {
        val portal = SourcePortal.getPortal((it.body() as JsonObject).getString("portal_uuid"))!!
        val representation = portal.tracesView
        representation.viewType = TracesView.Companion.ViewType.TRACES

        if (representation.innerTraceStack.size > 0) {
            representation.viewType = TracesView.Companion.ViewType.TRACE_STACK
            val stack = representation.innerTraceStack.pop()

            if (representation.innerTrace) {
                updateUI(portal)
            } else if (!portal.external) {
                //navigating back to parent stack
                //                    val rootArtifactQualifiedName = stack.getJsonObject(0).getString("root_artifact_qualified_name")
                //                    vertx.eventBus().send(
                //                        NAVIGATE_TO_ARTIFACT.address,
                //                        JsonObject().put("portal_uuid", portal.portalUuid)
                //                            .put("artifact_qualified_name", rootArtifactQualifiedName)
                //                            .put("parent_stack_navigation", true)
                //                    )
            } else {
                updateUI(portal)
            }
        } else {
            updateUI(portal)
        }
    }

    private fun clickedDisplayTraceStack(messageHandler: Message<JsonObject>) {
        val request = messageHandler.body() as JsonObject
        log.debug("Displaying trace stack: {}", request)

        if (request.getString("trace_id") == null) {
            val portal = SourcePortal.getPortal(request.getString("portal_uuid"))!!
            portal.tracesView.viewType = TracesView.Companion.ViewType.TRACE_STACK
            updateUI(portal)
        } else {
            vertx.eventBus().request<JsonArray>(GetTraceStack, request) {
                if (it.failed()) {
                    it.cause().printStackTrace()
                    log.error("Failed to display trace stack", it.cause())
                } else {
                    val portal = SourcePortal.getPortal(request.getString("portal_uuid"))!!
                    portal.tracesView.viewType = TracesView.Companion.ViewType.TRACE_STACK
                    portal.tracesView.traceStack = it.result().body() as JsonArray
                    portal.tracesView.traceId = request.getString("trace_id")
                    updateUI(portal)
                }
            }
        }
    }

    private fun tracesTabOpened(it: Message<JsonObject>) {
        log.info("Traces tab opened")
        val message = JsonObject.mapFrom(it.body())
        val portalUuid = message.getString("portal_uuid")
        val portal = SourcePortal.getPortal(portalUuid)
        if (portal == null) {
            log.warn("Ignoring traces tab opened event. Unable to find portal: {}", portalUuid)
            return
        }

        val orderType = message.getString("trace_order_type")
        if (orderType != null) {
            //user possibly changed current trace order type; todo: create event
            portal.tracesView.orderType = TraceOrderType.valueOf(orderType.toUpperCase())
        }
        portal.currentTab = thisTab
        SourcePortal.ensurePortalActive(portal)
        updateUI(portal)

        vertx.eventBus().send(RefreshTraces, portal)
    }

    private fun displayTraces(portal: SourcePortal) {
        if (portal.tracesView.artifactTraceResult != null) {
            val artifactTraceResult = portal.tracesView.artifactTraceResult!!
            vertx.eventBus().displayTraces(portal.portalUuid, artifactTraceResult)
            log.debug(
                "Displayed traces for artifact: {} - Type: {} - Trace size: {}",
                getShortQualifiedFunctionName(artifactTraceResult.artifactQualifiedName),
                artifactTraceResult.orderType,
                artifactTraceResult.traces.size
            )
        }
    }

    private fun displayTraceStack(portal: SourcePortal) {
        val representation = portal.tracesView
        val traceId = representation.traceId
        val traceStack = representation.traceStack

        if (representation.innerTrace && representation.viewType != TracesView.Companion.ViewType.SPAN_INFO) {
            val innerTraceStackInfo = InnerTraceStackInfo(
                innerLevel = representation.innerTraceStack.size,
                traceStack = representation.innerTraceStack.peek().toString()
            )
            vertx.eventBus().publish(
                portal.portalUuid + "-DisplayInnerTraceStack",
                JsonObject(Json.encode(innerTraceStackInfo))
            )
            log.info("Displayed inner trace stack. Stack size: {}", representation.innerTraceStack.peek().size())
        } else if (traceStack != null && !traceStack.isEmpty) {
            vertx.eventBus().displayTraceStack(portal.portalUuid, representation.traceStack!!)
            log.info("Displayed trace stack for id: {} - Stack size: {}", traceId, traceStack.size())
        }
    }

    private fun displaySpanInfo(portal: SourcePortal) {
        val traceId = portal.tracesView.traceId!!
        val spanId = portal.tracesView.spanId
        val representation = portal.tracesView
        val traceStack = if (representation.innerTrace) {
            representation.innerTraceStack.peek()
        } else {
            representation.getTraceStack(traceId)!!
        }

        for (i in 0 until traceStack.size()) {
            val span = traceStack.getJsonObject(i).getJsonObject("span")
            if (span.getInteger("span_id") == spanId) {
                val spanArtifactQualifiedName = span.getString("artifact_qualified_name")
                if (portal.external && span.getBoolean("has_child_stack")) {
//                    val spanStackQuery = TraceSpanStackQuery.builder()
//                        .oneLevelDeep(true).followExit(true)
//                        .segmentId(span.getString("segment_id"))
//                        .spanId(span.getLong("span_id"))
//                        .traceId(traceId).build()
//                    SourcePortalConfig.current.getCoreClient(portal.appUuid).getTraceSpans(portal.appUuid,
//                        portal.portalUI.viewingPortalArtifact, spanStackQuery, {
//                            if (it.failed()) {
////                                log.error("Failed to get trace spans", it.cause())
//                                vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
//                            } else {
//                                val queryResult = it.result()
//                                val spanTracesView = portal.portalUI.tracesView
//                                        spanTracesView.viewType = TracesView.Companion.ViewType.TRACE_STACK
//                                spanTracesView.innerTraceStack.push(
//                                    handleTraceStack(
//                                        portal.appUuid, portal.portalUI.viewingPortalArtifact, queryResult
//                                    )
//                                )
//
//                                displayTraceStack(portal)
//                            }
//                        })
                    break
                } else if (spanArtifactQualifiedName == null ||
                    spanArtifactQualifiedName == portal.viewingPortalArtifact
                ) {
                    vertx.eventBus().displaySpanInfo(portal.portalUuid, span)
                    log.info("Displayed trace span info: {}", span)
                } else {
//                    vertx.eventBus().request<Boolean>(
//                        CAN_NAVIGATE_TO_ARTIFACT.address, JsonObject()
//                            .put("app_uuid", portal.appUuid)
//                            .put("artifact_qualified_name", spanArtifactQualifiedName)
//                    ) {
//                        if (it.succeeded() && it.result().body() == true) {
//                            val spanStackQuery = TraceSpanStackQuery.builder()
//                                .oneLevelDeep(true).followExit(true)
//                                .segmentId(span.getString("segment_id"))
//                                .spanId(span.getLong("span_id"))
//                                .traceId(traceId).build()
//
//                            val spanPortal = SourcePortal.getInternalPortal(portal.appUuid, spanArtifactQualifiedName)
//                            if (!spanPortal.isPresent) {
////                                log.error("Failed to get span portal: {}", spanArtifactQualifiedName)
//                                vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
//                                return@request
//                            }
//
//                            //todo: cache
//                            SourcePortalConfig.current.getCoreClient(portal.appUuid).getTraceSpans(
//                                portal.appUuid,
//                                portal.portalUI.viewingPortalArtifact, spanStackQuery
//                            ) {
//                                if (it.failed()) {
////                                        log.error("Failed to get trace spans", it.cause())
//                                    vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
//                                } else {
//                                    //navigated away from portal; reset to trace stack
//                                    portal.portalUI.tracesView.viewType = TracesView.Companion.ViewType.TRACE_STACK
//
//                                    val queryResult = it.result()
//                                    val spanTracesView = spanPortal.get().portalUI.tracesView
//                                    spanTracesView.viewType = TracesView.Companion.ViewType.TRACE_STACK
//                                    spanTracesView.innerTraceStack.push(
//                                        handleTraceStack(
//                                            portal.appUuid, portal.portalUI.viewingPortalArtifact, queryResult
//                                        )
//                                    )
//                                    vertx.eventBus().send(
//                                        NAVIGATE_TO_ARTIFACT.address,
//                                        JsonObject().put("portal_uuid", spanPortal.get().portalUuid)
//                                            .put("artifact_qualified_name", spanArtifactQualifiedName)
//                                    )
//                                }
//                            }
//                        } else {
//                            vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
////                            log.info("Displayed trace span info: {}", span)
//                        }
//                    }
                }
            }
        }
    }

    fun handleArtifactTraceResult(artifactTraceResult: TraceResult) {
        handleArtifactTraceResult(
            SourcePortal.getPortals(
                artifactTraceResult.appUuid,
                artifactTraceResult.artifactQualifiedName
            ).toList(), artifactTraceResult
        )
    }

    fun handleArtifactTraceResult(portals: List<SourcePortal>, artifactTraceResult: TraceResult) {
        val traces = ArrayList<Trace>()
        artifactTraceResult.traces.forEach {
            traces.add(it.copy(prettyDuration = humanReadableDuration(Duration.ofMillis(it.duration.toLong()))))
        }
        val updatedArtifactTraceResult = artifactTraceResult.copy(
            traces = traces,
            artifactSimpleName = removePackageAndClassName(
                removePackageNames(artifactTraceResult.artifactQualifiedName)
            )
        )

        portals.forEach {
            val representation = it.tracesView
            representation.cacheArtifactTraceResult(updatedArtifactTraceResult)

            if (it.viewingPortalArtifact == updatedArtifactTraceResult.artifactQualifiedName
                && it.tracesView.viewType == TracesView.Companion.ViewType.TRACES
            ) {
                updateUI(it)
            }
        }
    }

    fun handleTraceStack(
        appUuid: String,
        rootArtifactQualifiedName: String,
        spanQueryResult: TraceSpanStackQueryResult
    ): JsonArray {
        val spanInfos = ArrayList<TraceSpanInfo>()
        val totalTime = spanQueryResult.traceSpans[0].endTime!! - spanQueryResult.traceSpans[0].startTime

        spanQueryResult.traceSpans.forEach { span ->
            val timeTookMs = span.endTime!! - span.startTime
            val timeTook = humanReadableDuration(Duration.ofMillis(timeTookMs))

            //detect if operation name is really an artifact name
            val finalSpan = if (QUALIFIED_NAME_PATTERN.matcher(span.endpointName!!).matches()) {
                span.copy(artifactQualifiedName = span.endpointName)
            } else {
                span
            }
            val operationName = if (finalSpan.artifactQualifiedName != null) {
                removePackageAndClassName(removePackageNames(finalSpan.artifactQualifiedName))
            } else {
                finalSpan.endpointName
            }

            spanInfos.add(
                TraceSpanInfo(
                    span = finalSpan,
                    appUuid = appUuid,
                    rootArtifactQualifiedName = rootArtifactQualifiedName,
                    operationName = operationName,
                    timeTook = timeTook,
                    totalTracePercent = if (totalTime == 0L) {
                        0.0
                    } else {
                        timeTookMs / totalTime * 100.0
                    }
                )
            )
        }
        return JsonArray(Json.encode(spanInfos))
    }

    private fun getTraceStack(messageHandler: Message<JsonObject>) {
        val request = messageHandler.body() as JsonObject
        val portalUuid = request.getString("portal_uuid")
        val appUuid = request.getString("app_uuid")
        val artifactQualifiedName = request.getString("artifact_qualified_name")
        val globalTraceId = request.getString("trace_id")
        log.trace(
            "Getting trace spans. Artifact qualified name: {} - Trace id: {}",
            getShortQualifiedFunctionName(artifactQualifiedName),
            globalTraceId
        )

        val portal = SourcePortal.getPortal(portalUuid)!!
        val representation = portal.tracesView
        val traceStack = representation.getTraceStack(globalTraceId)
        if (traceStack != null) {
            log.trace("Got trace spans: {} from cache - Stack size: {}", globalTraceId, traceStack.size())
            messageHandler.reply(traceStack)
        } else {
            vertx.eventBus().request<TraceSpanStackQueryResult>(QueryTraceStack, globalTraceId) {
                if (it.failed()) {
                    log.error("Failed to get trace spans", it.cause())
                } else {
                    representation.cacheTraceStack(
                        globalTraceId,
                        handleTraceStack(appUuid, artifactQualifiedName, it.result().body())
                    )
                    messageHandler.reply(representation.getTraceStack(globalTraceId))
                }
            }
            //                val traceStackQuery = TraceSpanStackQuery.builder()
            //                    .oneLevelDeep(true)
            //                    .traceId(globalTraceId).build()
            //                SourcePortalConfig.current.getCoreClient(appUuid)
            //                    .getTraceSpans(appUuid, artifactQualifiedName, traceStackQuery) {
            //                        if (it.failed()) {
            ////                        log.error("Failed to get trace spans", it.cause())
            //                        } else {
            //                            representation.cacheTraceStack(
            //                                globalTraceId, handleTraceStack(
            //                                    appUuid, artifactQualifiedName, it.result()
            //                                )
            //                            )
            //                            messageHandler.reply(representation.getTraceStack(globalTraceId))
            ////                        context.stop()
            //                        }
            //                    }
        }
    }

    private fun humanReadableDuration(duration: Duration): String {
        if (duration.seconds < 1) {
            return duration.toMillis().toString() + "ms"
        }
        return duration.toString().substring(2)
            .replace("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase()
    }
}
