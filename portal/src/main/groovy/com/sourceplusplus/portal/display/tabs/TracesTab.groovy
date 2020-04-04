package com.sourceplusplus.portal.display.tabs

import com.sourceplusplus.api.model.config.SourcePortalConfig
import com.sourceplusplus.api.model.internal.InnerTraceStackInfo
import com.sourceplusplus.api.model.internal.TraceSpanInfo
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.portal.PortalBootstrap
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import com.sourceplusplus.portal.display.PortalTab
import com.sourceplusplus.portal.display.tabs.views.TracesView
import groovy.util.logging.Slf4j
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.*

/**
 * Displays traces (and the underlying spans) for a given source code artifact.
 *
 * @version 0.2.5
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class TracesTab extends AbstractTab {

    public static final String TRACES_TAB_OPENED = "TracesTabOpened"
    public static final String GET_TRACE_STACK = "GetTraceStack"
    public static final String CLICKED_DISPLAY_TRACES = "ClickedDisplayTraces"
    public static final String CLICKED_DISPLAY_TRACE_STACK = "ClickedDisplayTraceStack"
    public static final String CLICKED_DISPLAY_SPAN_INFO = "ClickedDisplaySpanInfo"
    public static final String DISPLAY_TRACES = "DisplayTraces"
    public static final String DISPLAY_TRACE_STACK = "DisplayTraceStack"
    public static final String DISPLAY_SPAN_INFO = "DisplaySpanInfo"

    private static final Pattern QUALIFIED_NAME_PATTERN = Pattern.compile('.+\\..+\\(.*\\)')

    TracesTab() {
        super(PortalTab.Traces)
    }

    @Override
    void start() throws Exception {
        super.start()

        //refresh with traces from cache (if avail)
        vertx.eventBus().consumer(TRACES_TAB_OPENED, {
            log.info("Traces tab opened")
            def message = JsonObject.mapFrom(it.body())
            def portalUuid = message.getString("portal_uuid")
            def portal = SourcePortal.getPortal(portalUuid)
            if (portal == null) {
                log.warn("Ignoring traces tab opened event. Unable to find portal: $portalUuid")
                return
            }

            def orderType = message.getString("trace_order_type")
            if (orderType) {
                //user possibly changed current trace order type; todo: create event
                portal.portalUI.tracesView.orderType = TraceOrderType.valueOf(orderType.toUpperCase())
            }
            portal.portalUI.currentTab = PortalTab.Traces
            SourcePortal.ensurePortalActive(portal)
            updateUI(portal)

            //subscribe (re-subscribe) to get latest stats
            def subscribeRequest = ArtifactTraceSubscribeRequest.builder()
                    .appUuid(portal.appUuid)
                    .artifactQualifiedName(portal.portalUI.viewingPortalArtifact)
                    .addOrderTypes(TraceOrderType.LATEST_TRACES, TraceOrderType.SLOWEST_TRACES)
                    .build()
            SourcePortalConfig.current.getCoreClient(portal.appUuid).subscribeToArtifact(subscribeRequest, {
                if (it.succeeded()) {
                    log.info("Successfully subscribed to traces with request: " + subscribeRequest)
                } else {
                    log.error("Failed to subscribe to artifact traces", it.cause())
                }
            })
        })
        vertx.eventBus().consumer(ARTIFACT_TRACE_UPDATED.address, {
            handleArtifactTraceResult(it.body() as ArtifactTraceResult)
        })

        //external portals hold more traces;
        //this will prepopulate those portals and ensure slowest traces remain current
        vertx.eventBus().consumer(TRACES_TAB_OPENED, {
            def portalUuid = JsonObject.mapFrom(it.body()).getString("portal_uuid")
            def portal = SourcePortal.getPortal(portalUuid)
            if (portal == null) {
                log.warn("Ignoring traces tab opened event. Unable to find portal: $portalUuid")
            } else if (portal.external) {
                def traceQuery = TraceQuery.builder().orderType(portal.portalUI.tracesView.orderType)
                        .pageSize(25)
                        .appUuid(portal.appUuid)
                        .artifactQualifiedName(portal.portalUI.viewingPortalArtifact)
                        .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
                        .durationStop(Instant.now())
                        .durationStep("SECOND").build()
                SourcePortalConfig.current.getCoreClient(portal.appUuid).getTraces(traceQuery, {
                    if (it.succeeded()) {
                        def traceResult = ArtifactTraceResult.builder()
                                .appUuid(traceQuery.appUuid())
                                .artifactQualifiedName(traceQuery.artifactQualifiedName())
                                .orderType(traceQuery.orderType())
                                .start(traceQuery.durationStart())
                                .stop(traceQuery.durationStop())
                                .step(traceQuery.durationStep())
                                .traces(it.result().traces())
                                .total(it.result().total())
                                .build()
                        handleArtifactTraceResult(Collections.singletonList(portal), traceResult)
                    } else {
                        log.error("Failed to get traces", it.cause())
                    }
                })
            }
        })
        vertx.setPeriodic(60_000, {
            SourcePortal.getExternalPortals().each {
                if (it.portalUI.currentTab == PortalTab.Traces
                        && it.portalUI.tracesView.orderType == TraceOrderType.SLOWEST_TRACES) {
                    def traceQuery = TraceQuery.builder().orderType(it.portalUI.tracesView.orderType)
                            .pageSize(25)
                            .appUuid(it.appUuid)
                            .artifactQualifiedName(it.portalUI.viewingPortalArtifact)
                            .durationStart(Instant.now().minus(30, ChronoUnit.DAYS))
                            .durationStop(Instant.now())
                            .durationStep("SECOND").build()
                    SourcePortalConfig.current.getCoreClient(it.appUuid).getTraces(traceQuery, {
                        if (it.succeeded()) {
                            def traceResult = ArtifactTraceResult.builder()
                                    .appUuid(traceQuery.appUuid())
                                    .artifactQualifiedName(traceQuery.artifactQualifiedName())
                                    .orderType(traceQuery.orderType())
                                    .start(traceQuery.durationStart())
                                    .stop(traceQuery.durationStop())
                                    .step(traceQuery.durationStep())
                                    .traces(it.result().traces())
                                    .total(it.result().total())
                                    .build()
                            handleArtifactTraceResult(traceResult)
                        } else {
                            log.error("Failed to get traces", it.cause())
                        }
                    })
                }
            }
        })

        //user viewing portal under new artifact
        vertx.eventBus().consumer(PortalViewTracker.CHANGED_PORTAL_ARTIFACT, {
//            def portal = SourcePortal.getPortal(JsonObject.mapFrom(it.body()).getString("portal_uuid"))
//            vertx.eventBus().send(portal.portalUuid + "-ClearTraceStack", new JsonObject())
        })

        //user clicked into trace stack
        vertx.eventBus().consumer(CLICKED_DISPLAY_TRACE_STACK, { messageHandler ->
            def request = messageHandler.body() as JsonObject
            log.debug("Displaying trace stack: " + request)

            if (request.getString("trace_id") == null) {
                def portal = SourcePortal.getPortal(request.getString("portal_uuid"))
                portal.portalUI.tracesView.viewType = TracesView.ViewType.TRACE_STACK
                updateUI(portal)
            } else {
                vertx.eventBus().request(GET_TRACE_STACK, request, {
                    if (it.failed()) {
                        it.cause().printStackTrace()
                        log.error("Failed to display trace stack", it.cause())
                    } else {
                        def portal = SourcePortal.getPortal(request.getString("portal_uuid"))
                        portal.portalUI.tracesView.viewType = TracesView.ViewType.TRACE_STACK
                        portal.portalUI.tracesView.traceStack = it.result().body() as JsonArray
                        portal.portalUI.tracesView.traceId = request.getString("trace_id")
                        updateUI(portal)
                    }
                })
            }
        })

        vertx.eventBus().consumer(CLICKED_DISPLAY_TRACES, {
            def portal = SourcePortal.getPortal((it.body() as JsonObject).getString("portal_uuid"))
            def representation = portal.portalUI.tracesView
            representation.viewType = TracesView.ViewType.TRACES

            if (representation.rootArtifactQualifiedName == null) {
                representation.innerTrace = false
                representation.innerLevel = 0
                updateUI(portal)
            } else {
                //navigating back to parent stack
                vertx.eventBus().send(NAVIGATE_TO_ARTIFACT.address,
                        new JsonObject().put("portal_uuid", portal.portalUuid)
                                .put("artifact_qualified_name", representation.rootArtifactQualifiedName)
                                .put("parent_stack_navigation", true)
                )
            }
        })

        //user clicked into span
        vertx.eventBus().consumer(CLICKED_DISPLAY_SPAN_INFO, { messageHandler ->
            def spanInfoRequest = messageHandler.body() as JsonObject
            log.debug("Clicked display span info: " + spanInfoRequest)

            def portalUuid = spanInfoRequest.getString("portal_uuid")
            def portal = SourcePortal.getPortal(portalUuid)
            def representation = portal.portalUI.tracesView
            representation.viewType = TracesView.ViewType.SPAN_INFO
            representation.traceId = spanInfoRequest.getString("trace_id")
            representation.spanId = spanInfoRequest.getInteger("span_id")
            updateUI(portal)
        })

        //query core for trace stack (or get from cache)
        vertx.eventBus().consumer(GET_TRACE_STACK, { messageHandler ->
            def timer = PortalBootstrap.portalMetrics.timer(GET_TRACE_STACK)
            def context = timer.time()
            def request = messageHandler.body() as JsonObject
            def portalUuid = request.getString("portal_uuid")
            def appUuid = request.getString("app_uuid")
            def artifactQualifiedName = request.getString("artifact_qualified_name")
            def globalTraceId = request.getString("trace_id")
            log.trace("Getting trace spans. Artifact qualified name: %s - Trace id: %s",
                    artifactQualifiedName, globalTraceId)

            def portal = SourcePortal.getPortal(portalUuid)
            def representation = portal.portalUI.tracesView
            def traceStack = representation.getTraceStack(globalTraceId)
            if (traceStack != null) {
                log.trace("Got trace spans: $globalTraceId from cache - Stack size: " + traceStack.size())
                messageHandler.reply(traceStack)
                context.stop()
            } else {
                def traceStackQuery = TraceSpanStackQuery.builder()
                        .oneLevelDeep(true)
                        .traceId(globalTraceId).build()
                SourcePortalConfig.current.getCoreClient(appUuid).getTraceSpans(appUuid, artifactQualifiedName, traceStackQuery, {
                    if (it.failed()) {
                        log.error("Failed to get trace spans", it.cause())
                    } else {
                        representation.cacheTraceStack(globalTraceId, handleTraceStack(
                                appUuid, artifactQualifiedName, it.result()))
                        messageHandler.reply(representation.getTraceStack(globalTraceId))
                        context.stop()
                    }
                })
            }
        })
        log.info("{} started", getClass().getSimpleName())
    }

    @Override
    void updateUI(SourcePortal portal) {
        if (portal.portalUI.currentTab != thisTab) {
            return
        }

        switch (portal.portalUI.tracesView.viewType) {
            case TracesView.ViewType.TRACES:
                displayTraces(portal)
                break
            case TracesView.ViewType.TRACE_STACK:
                displayTraceStack(portal)
                break
            case TracesView.ViewType.SPAN_INFO:
                displaySpanInfo(portal)
                break
        }
    }

    private void displayTraces(SourcePortal portal) {
        if (portal.portalUI.tracesView.artifactTraceResult) {
            def artifactTraceResult = portal.portalUI.tracesView.artifactTraceResult
            vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_TRACES",
                    new JsonObject(Json.encode(artifactTraceResult)))
            log.debug("Displayed traces for artifact: " + artifactTraceResult.artifactQualifiedName()
                    + " - Type: " + artifactTraceResult.orderType()
                    + " - Trace size: " + artifactTraceResult.traces().size())
        }
    }

    private void displayTraceStack(SourcePortal portal) {
        def representation = portal.portalUI.tracesView
        def traceId = representation.traceId
        def traceStack = representation.traceStack

        if (traceStack && !traceStack.isEmpty()) {
            vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_TRACE_STACK", representation.traceStack)
            log.info("Displayed trace stack for id: $traceId - Stack size: " + traceStack.size())
        } else if (representation.innerTrace && representation.viewType != TracesView.ViewType.SPAN_INFO) {
            def innerTraceStackInfo = InnerTraceStackInfo.builder()
                    .innerLevel(representation.innerLevel)
                    .traceStack(representation.innerTraceStack).build()
            vertx.eventBus().publish(portal.portalUuid + "-DisplayInnerTraceStack",
                    new JsonObject(Json.encode(innerTraceStackInfo))
            )
            log.info("Displayed inner trace stack. Stack size: " + representation.innerTraceStack.size())
        }
    }

    private void displaySpanInfo(SourcePortal portal) {
        def traceId = portal.portalUI.tracesView.traceId
        def spanId = portal.portalUI.tracesView.spanId
        def representation = portal.portalUI.tracesView
        def traceStack
        if (representation.innerTrace) {
            traceStack = representation.innerTraceStack
        } else {
            traceStack = representation.getTraceStack(traceId)
        }

        for (int i = 0; i < traceStack.size(); i++) {
            def span = traceStack.getJsonObject(i).getJsonObject("span")
            if (span.getInteger("span_id") == spanId) {
                def spanArtifactQualifiedName = span.getString("artifact_qualified_name")
                if (portal.external
                        || spanArtifactQualifiedName == null
                        || spanArtifactQualifiedName == portal.portalUI.viewingPortalArtifact) {
                    vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                    log.info("Displayed trace span info: " + span)
                } else {
                    vertx.eventBus().request(CAN_NAVIGATE_TO_ARTIFACT.address, new JsonObject()
                            .put("app_uuid", portal.appUuid)
                            .put("artifact_qualified_name", spanArtifactQualifiedName), {
                        if (it.succeeded() && it.result().body() == true) {
                            def spanStackQuery = TraceSpanStackQuery.builder()
                                    .oneLevelDeep(true).followExit(true)
                                    .segmentId(span.getString("segment_id"))
                                    .spanId(span.getLong("span_id"))
                                    .traceId(traceId).build()

                            def spanPortal = SourcePortal.getInternalPortal(portal.appUuid, spanArtifactQualifiedName)
                            if (!spanPortal.isPresent()) {
                                log.error("Failed to get span portal:" + spanArtifactQualifiedName)
                                vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                                return
                            }

                            //todo: cache
                            SourcePortalConfig.current.getCoreClient(portal.appUuid).getTraceSpans(portal.appUuid,
                                    portal.portalUI.viewingPortalArtifact, spanStackQuery, {
                                if (it.failed()) {
                                    log.error("Failed to get trace spans", it.cause())
                                    vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                                } else {
                                    //navigated away from portal; reset to trace stack
                                    portal.portalUI.tracesView.viewType = TracesView.ViewType.TRACE_STACK

                                    def queryResult = it.result()
                                    def innerLevel = representation.innerLevel + 1
                                    def spanTracesView = spanPortal.get().portalUI.tracesView
                                    if (span.getString("type") == "Exit"
                                            && queryResult.traceSpans().get(0).type() == "Entry") {
                                        innerLevel = 0
                                    } else {
                                        spanTracesView.rootArtifactQualifiedName = portal.portalUI.viewingPortalArtifact
                                    }
                                    spanTracesView.viewType = TracesView.ViewType.TRACE_STACK
                                    spanTracesView.innerTrace = true
                                    spanTracesView.innerLevel = innerLevel
                                    spanTracesView.innerTraceStack = handleTraceStack(
                                            portal.appUuid, portal.portalUI.viewingPortalArtifact, queryResult)
                                    vertx.eventBus().send(NAVIGATE_TO_ARTIFACT.address,
                                            new JsonObject().put("portal_uuid", spanPortal.get().portalUuid)
                                                    .put("artifact_qualified_name", spanArtifactQualifiedName))
                                }
                            })
                        } else {
                            vertx.eventBus().send(portal.portalUuid + "-$DISPLAY_SPAN_INFO", span)
                            log.info("Displayed trace span info: " + span)
                        }
                    })
                }
            }
        }
    }

    private void handleArtifactTraceResult(ArtifactTraceResult artifactTraceResult) {
        handleArtifactTraceResult(SourcePortal.getPortals(artifactTraceResult.appUuid(),
                artifactTraceResult.artifactQualifiedName()).collect(), artifactTraceResult)
    }

    private void handleArtifactTraceResult(List<SourcePortal> portals, ArtifactTraceResult artifactTraceResult) {
        def traces = new ArrayList<Trace>()
        artifactTraceResult.traces().each {
            traces.add(it.withPrettyDuration(humanReadableDuration(Duration.ofMillis(it.duration()))))
        }
        artifactTraceResult = artifactTraceResult.withTraces(traces)
                .withArtifactSimpleName(removePackageAndClassName(removePackageNames(artifactTraceResult.artifactQualifiedName())))

        portals.each {
            def representation = it.portalUI.tracesView
            representation.cacheArtifactTraceResult(artifactTraceResult)

            if (it.portalUI.viewingPortalArtifact == artifactTraceResult.artifactQualifiedName()
                    && it.portalUI.tracesView.viewType == TracesView.ViewType.TRACES) {
                updateUI(it)
            }
        }
    }

    private static JsonArray handleTraceStack(String appUuid, String rootArtifactQualifiedName,
                                              TraceSpanStackQueryResult spanQueryResult) {
        def spanInfos = new ArrayList<TraceSpanInfo>()
        def totalTime = spanQueryResult.traceSpans().get(0).endTime() - spanQueryResult.traceSpans().get(0).startTime()

        for (def span : spanQueryResult.traceSpans()) {
            def timeTookMs = span.endTime() - span.startTime()
            def timeTook = humanReadableDuration(Duration.ofMillis(timeTookMs))
            def spanInfo = TraceSpanInfo.builder()
                    .span(span)
                    .appUuid(appUuid)
                    .rootArtifactQualifiedName(rootArtifactQualifiedName)
                    .timeTook(timeTook)
                    .totalTracePercent((totalTime == 0) ? 0d : timeTookMs / totalTime * 100.0d)

            //detect if operation name is really an artifact name
            if (QUALIFIED_NAME_PATTERN.matcher(span.endpointName()).matches()) {
                spanInfo.span(span = span.withArtifactQualifiedName(span.endpointName()))
            }
            if (span.artifactQualifiedName()) {
                spanInfo.operationName(removePackageAndClassName(removePackageNames(span.artifactQualifiedName())))
            } else {
                spanInfo.operationName(span.endpointName())
            }
            spanInfos.add(spanInfo.build())
        }
        return new JsonArray(Json.encode(spanInfos))
    }

    static String removePackageNames(String qualifiedMethodName) {
        if (!qualifiedMethodName || qualifiedMethodName.indexOf('.') == -1) return qualifiedMethodName
        def className = qualifiedMethodName.substring(0, qualifiedMethodName.substring(
                0, qualifiedMethodName.indexOf("(")).lastIndexOf("."))
        if (className.contains('$')) {
            className = className.substring(0, className.indexOf('$'))
        }

        def arguments = qualifiedMethodName.substring(qualifiedMethodName.indexOf("("))
        def argArray = arguments.substring(1, arguments.length() - 1).split(",")
        def argText = "("
        for (def i = 0; i < argArray.length; i++) {
            def qualifiedArgument = argArray[i]
            def newArgText = qualifiedArgument.substring(qualifiedArgument.lastIndexOf(".") + 1)
            if (qualifiedArgument.startsWith(className + '$')) {
                newArgText = qualifiedArgument.substring(qualifiedArgument.lastIndexOf('$') + 1)
            }
            argText += newArgText

            if ((i + 1) < argArray.length) {
                argText += ","
            }
        }
        argText += ")"

        def methodNameArr = qualifiedMethodName.substring(0, qualifiedMethodName.indexOf("(")).split('\\.')
        if (methodNameArr.length == 1) {
            return methodNameArr[0] + argText
        } else {
            return methodNameArr[methodNameArr.length - 2] + '.' + methodNameArr[methodNameArr.length - 1] + argText
        }
    }

    static removePackageAndClassName(String qualifiedMethodName) {
        if (!qualifiedMethodName || qualifiedMethodName.indexOf('.') == -1 || qualifiedMethodName.indexOf('(') == -1) {
            return qualifiedMethodName
        }
        return qualifiedMethodName.substring(qualifiedMethodName.substring(
                0, qualifiedMethodName.indexOf("(")).lastIndexOf(".") + 1)
    }

    static String humanReadableDuration(Duration duration) {
        if (duration.seconds < 1) {
            return duration.toMillis() + "ms"
        }
        return duration.toString().substring(2)
                .replaceAll('(\\d[HMS])(?!$)', '$1 ')
                .toLowerCase()
    }
}
