package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.extensions.displayTraceSpan
import com.sourceplusplus.portal.extensions.displayTraceStack
import com.sourceplusplus.portal.extensions.displayTraces
import com.sourceplusplus.protocol.portal.PageType
import com.sourceplusplus.portal.model.TraceDisplayType.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.TraceSpanUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.ArtifactTracesUpdated
import com.sourceplusplus.protocol.ProtocolAddress.Global.CanNavigateToArtifact
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplayInnerTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplayTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClosePortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.FetchMoreTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.FindPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.GetTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.NavigateToArtifact
import com.sourceplusplus.protocol.ProtocolAddress.Global.OpenPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.QueryTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.RefreshTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetTraceOrderType
import com.sourceplusplus.protocol.ProtocolAddress.Portal.RenderPage
import com.sourceplusplus.protocol.ProtocolAddress.Portal.UpdateTraceSpan
import com.sourceplusplus.protocol.artifact.ArtifactQualifiedName
import com.sourceplusplus.protocol.artifact.ArtifactType.METHOD
import com.sourceplusplus.protocol.artifact.trace.*
import com.sourceplusplus.protocol.utils.ArtifactNameUtils.getShortQualifiedFunctionName
import com.sourceplusplus.protocol.utils.ArtifactNameUtils.removePackageAndClassName
import com.sourceplusplus.protocol.utils.ArtifactNameUtils.removePackageNames
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import kotlin.time.ExperimentalTime

/**
 * Displays traces (and the underlying spans) for a given source code artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class TracesDisplay : AbstractDisplay(PageType.TRACES) {

    companion object {
        private val log = LoggerFactory.getLogger(TracesDisplay::class.java)
        val QUALIFIED_NAME_PATTERN = Pattern.compile(".+\\..+\\(.*\\)")!!
    }

    override suspend fun start() {
        vertx.setPeriodic(5000) {
            SourcePortal.getPortals().filter {
                it.configuration.currentPage == PageType.TRACES && (it.visible || it.configuration.external)
            }.forEach {
                vertx.eventBus().send(RefreshTraces, it)
            }
        }

        //plugin listeners
        vertx.eventBus().consumer<TraceResult>(ArtifactTracesUpdated) { handleArtifactTraceResult(it.body()) }
        vertx.eventBus().consumer<TraceSpan>(TraceSpanUpdated) { handleTraceSpanUpdated(it.body()) }

        //portal listeners
        vertx.eventBus().consumer(SetTraceOrderType, this@TracesDisplay::setTraceOrderType)
        vertx.eventBus().consumer(FetchMoreTraces, this@TracesDisplay::fetchMoreTraces)
        vertx.eventBus().consumer(ClickedDisplayTraceStack, this@TracesDisplay::clickedDisplayTraceStack)
        vertx.eventBus().consumer(ClickedDisplayInnerTraceStack, this@TracesDisplay::clickedDisplayInnerTraceStack)
        vertx.eventBus().consumer(ClickedDisplayTraces, this@TracesDisplay::clickedDisplayTraces)
        vertx.eventBus().consumer(ClickedDisplaySpanInfo, this@TracesDisplay::clickedDisplaySpanInfo)
        vertx.eventBus().consumer(GetTraceStack, this@TracesDisplay::getTraceStack)

        log.info("{} started", javaClass.simpleName)
    }

    override fun updateUI(portal: SourcePortal) {
        if (portal.configuration.currentPage != thisTab) {
            return
        }

        when (portal.tracesView.viewType) {
            TRACES -> displayTraces(portal)
            TRACE_STACK -> displayTraceStack(portal)
            SPAN_INFO -> displaySpanInfo(portal)
        }
    }

    private fun fetchMoreTraces(messageHandler: Message<JsonObject>) {
        val portalUuid = messageHandler.body().getString("portalUuid")
        val pageNumber = messageHandler.body().getInteger("pageNumber")
        val portal = SourcePortal.getPortal(portalUuid)!!
        if (pageNumber == null) {
            portal.tracesView.pageNumber++
        } else {
            portal.tracesView.pageNumber = pageNumber
        }
        vertx.eventBus().send(RefreshTraces, portal)
    }

    private fun clickedDisplaySpanInfo(messageHandler: Message<JsonObject>) {
        val spanInfoRequest = messageHandler.body() as JsonObject
        log.debug("Clicked display span info: {}", spanInfoRequest)

        val portalUuid = spanInfoRequest.getString("portalUuid")
        val portal = SourcePortal.getPortal(portalUuid)!!
        val representation = portal.tracesView
        representation.viewType = SPAN_INFO
        representation.traceId = spanInfoRequest.getString("traceId")
        representation.spanId = spanInfoRequest.getInteger("spanId")
        updateUI(portal)
    }

    private fun clickedDisplayTraces(it: Message<JsonObject>) {
        val portal = SourcePortal.getPortal((it.body() as JsonObject).getString("portalUuid"))!!
        val representation = portal.tracesView
        representation.viewType = TRACES

        if (representation.traceStackPath?.getCurrentRoot() != null) {
            representation.viewType = TRACE_STACK
            representation.traceStackPath!!.removeLastRoot()

            if (!portal.configuration.external) {
                //navigating back to parent stack
                val artifactQualifiedName = ArtifactQualifiedName(
                    representation.traceStackPath!!.getCurrentRoot()?.artifactQualifiedName
                        ?: representation.rootArtifactQualifiedName!!, "", METHOD
                )

                vertx.eventBus().send(ClosePortal, portal)
                vertx.eventBus().send(NavigateToArtifact, artifactQualifiedName)
                vertx.eventBus().request<SourcePortal?>(FindPortal, artifactQualifiedName) {
                    val navPortal = it.result().body()!!
                    navPortal.configuration.currentPage = PageType.TRACES
                    vertx.eventBus().send(OpenPortal, navPortal)
                }
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

        if (request.getString("traceId") == null) {
            val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
            portal.tracesView.viewType = TRACE_STACK
            updateUI(portal)
        } else {
            vertx.eventBus().request<TraceStack>(GetTraceStack, request) {
                if (it.failed()) {
                    it.cause().printStackTrace()
                    log.error("Failed to display trace stack", it.cause())
                } else {
                    val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
                    portal.tracesView.viewType = TRACE_STACK
                    portal.tracesView.traceStack = it.result().body()
                    portal.tracesView.traceStackPath =
                        TraceStackPath(it.result().body(), orderType = portal.tracesView.orderType)
                    portal.tracesView.traceId = request.getString("traceId")
                    updateUI(portal)
                }
            }
        }
    }

    private fun clickedDisplayInnerTraceStack(messageHandler: Message<JsonObject>) {
        val request = messageHandler.body() as JsonObject
        log.debug("Displaying inner trace stack: {}", request)

        val portal = SourcePortal.getPortal(request.getString("portalUuid"))!!
        portal.tracesView.viewType = TRACE_STACK
        portal.tracesView.traceStackPath!!.follow(request.getString("segmentId"), request.getInteger("spanId"))

        if (!portal.configuration.external) {
            val traceSpan = portal.tracesView.traceStackPath!!.getCurrentRoot()!!
            val artifactQualifiedName = ArtifactQualifiedName(traceSpan.artifactQualifiedName!!, "", METHOD)
            vertx.eventBus().request<Boolean>(CanNavigateToArtifact, artifactQualifiedName) {
                if (it.succeeded()) {
                    if (it.result().body()) {
                        vertx.eventBus().send(ClosePortal, portal)
                        vertx.eventBus().send(NavigateToArtifact, artifactQualifiedName)
                        vertx.eventBus().request<SourcePortal?>(FindPortal, artifactQualifiedName) {
                            val navPortal = it.result().body()!!
                            navPortal.configuration.currentPage = PageType.TRACES
                            navPortal.tracesView.cloneView(portal.tracesView)
                            navPortal.tracesView.innerTraceStack = true
                            if (navPortal.tracesView.rootArtifactQualifiedName == null) {
                                navPortal.tracesView.rootArtifactQualifiedName = portal.viewingPortalArtifact
                            }
                            portal.tracesView.traceStackPath!!.removeLastRoot()
                            vertx.eventBus().send(OpenPortal, navPortal)
                        }
                    } else {
                        SourcePortal.ensurePortalActive(portal)
                        updateUI(portal)
                    }
                } else {
                    log.error("Failed to determine if artifact is navigable", it.cause())
                }
            }
        } else {
            SourcePortal.ensurePortalActive(portal)
            updateUI(portal)
        }
    }

    private fun setTraceOrderType(it: Message<JsonObject>) {
        log.info("Changed trace order type")
        val message = JsonObject.mapFrom(it.body())
        val portalUuid = message.getString("portalUuid")
        val portal = SourcePortal.getPortal(portalUuid)
        if (portal == null) {
            log.warn("Ignoring traces tab opened event. Unable to find portal: {}", portalUuid)
            return
        }

        if (portal.configuration.currentPage != PageType.TRACES) {
            portal.configuration.currentPage = thisTab
            vertx.eventBus().send(RenderPage(portal.portalUuid), JsonObject.mapFrom(portal.configuration))
        }

        val orderType = message.getString("traceOrderType")!!
        portal.tracesView.orderType = TraceOrderType.valueOf(orderType.toUpperCase())

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
        vertx.eventBus().displayTraceStack(portal.portalUuid, representation.traceStackPath!!)
        log.info("Displayed trace stack path for id: {}", representation.traceId)
    }

    private fun displaySpanInfo(portal: SourcePortal) {
        val traceId = portal.tracesView.traceId!!
        val spanId = portal.tracesView.spanId
        val representation = portal.tracesView
        val traceStack = representation.getTraceStack(traceId)!!

        for (i in 0 until traceStack.size()) {
            val span = traceStack.traceSpans[i]
            if (span.spanId == spanId) {
                val spanArtifactQualifiedName = span.meta["artifactQualifiedName"]
                if (spanArtifactQualifiedName == null || spanArtifactQualifiedName == portal.viewingPortalArtifact) {
                    vertx.eventBus().displayTraceSpan(portal.portalUuid, span)
                    log.info("Displayed trace span info: {}", span)
                }
            }
        }
    }

    private fun handleArtifactTraceResult(artifactTraceResult: TraceResult) {
        handleArtifactTraceResult(
            SourcePortal.getPortals(
                artifactTraceResult.appUuid,
                artifactTraceResult.artifactQualifiedName
            ).toList(), artifactTraceResult
        )
    }

    private fun handleTraceSpanUpdated(traceSpan: TraceSpan) {
        SourcePortal.getPortals(
            "null",
            traceSpan.artifactQualifiedName!!
        ).toList().forEach {
            it.tracesView.resolvedEndpointNames[traceSpan.traceId] = traceSpan.endpointName!!
            vertx.eventBus().publish(UpdateTraceSpan(it.portalUuid), JsonObject(Json.encode(traceSpan)))
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun handleArtifactTraceResult(portals: List<SourcePortal>, artifactTraceResult: TraceResult) {
        val updatedArtifactTraceResult = artifactTraceResult.copy(
            artifactSimpleName = removePackageAndClassName(
                removePackageNames(artifactTraceResult.artifactQualifiedName)
            )
        )

        portals.forEach {
            val representation = it.tracesView
            representation.cacheArtifactTraceResult(updatedArtifactTraceResult)

            if (it.viewingPortalArtifact == updatedArtifactTraceResult.artifactQualifiedName
                && it.tracesView.viewType == TRACES
            ) {
                updateUI(it)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun handleTraceStack(
        appUuid: String,
        rootArtifactQualifiedName: String,
        spanQueryResult: TraceSpanStackQueryResult
    ): TraceStack {
        val spanInfos = ArrayList<TraceSpan>()
        val totalTime = spanQueryResult.traceSpans[0].endTime - spanQueryResult.traceSpans[0].startTime

        spanQueryResult.traceSpans.forEach { span ->
            val timeTook = span.endTime - span.startTime

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
            }!!

            spanInfos.add(
                finalSpan.apply {
                    putMetaString("appUuid", appUuid)
                    putMetaString("rootArtifactQualifiedName", rootArtifactQualifiedName)
                    putMetaString("operationName", operationName)
                    putMetaDouble(
                        "totalTracePercent", if (totalTime.toLongMilliseconds() == 0L) 0.0
                        else timeTook / totalTime * 100.0
                    )
                }
            )
        }
        return TraceStack(spanInfos)
    }

    private fun getTraceStack(messageHandler: Message<JsonObject>) {
        val request = messageHandler.body() as JsonObject
        val portalUuid = request.getString("portalUuid")
        val appUuid = request.getString("appUuid")
        val artifactQualifiedName = request.getString("artifactQualifiedName")
        val globalTraceId = request.getString("traceId")
        log.trace(
            "Getting trace spans. Artifact qualified name: {} - Trace id: {}",
            getShortQualifiedFunctionName(artifactQualifiedName), globalTraceId
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
        }
    }
}
