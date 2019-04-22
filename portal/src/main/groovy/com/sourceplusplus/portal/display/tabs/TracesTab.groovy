package com.sourceplusplus.portal.display.tabs

import com.sourceplusplus.api.bridge.PluginBridgeEndpoints
import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.internal.InnerTraceStackInfo
import com.sourceplusplus.api.model.internal.TraceSpanInfo
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.portal.PortalBootstrap
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.portal.coordinate.track.PortalViewTracker
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.regex.Pattern

/**
 * Displays traces (and the underlying spans) for a given source code artifact.
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TracesTab extends AbstractVerticle {

    public static final String TRACES_TAB_OPENED = "TracesTabOpened"
    public static final String UPDATE_DISPLAYED_TRACES = "UpdateDisplayedTraces"
    public static final String GET_TRACE_STACK = "GetTraceStack"
    public static final String CLICKED_DISPLAY_TRACE_STACK = "ClickedDisplayTraceStack"
    public static final String CLICKED_DISPLAY_SPAN_INFO = "ClickedDisplaySpanInfo"
    public static final String CLICKED_GO_BACK_TO_TRACES = "ClickedGoBackToTraces"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static final Pattern QUALIFIED_NAME_PATTERN = Pattern.compile('.+\\..+\\(.*\\)')
    private final SourceCoreClient coreClient
    private final boolean pluginAvailable

    TracesTab(SourceCoreClient coreClient, boolean pluginAvailable) {
        this.coreClient = Objects.requireNonNull(coreClient)
        this.pluginAvailable = pluginAvailable
    }

    @Override
    void start() throws Exception {
        //refresh with traces from cache (if avail)
        vertx.eventBus().consumer(TRACES_TAB_OPENED, {
            log.info("Traces tab opened")

            if (pluginAvailable) {
                def message = JsonObject.mapFrom(it.body())
                def portalId = message.getInteger("portal_id")
                def portal = SourcePortal.getPortal(portalId)
                portal.interface.tracesView.orderType = TraceOrderType.valueOf(
                        message.getString("trace_order_type").toUpperCase())

                if (portal.interface.tracesView.artifactTraceResult) {
                    vertx.eventBus().send(UPDATE_DISPLAYED_TRACES, portal.interface.tracesView.artifactTraceResult)
                }
            } else {
                def subscriptions = config().getJsonArray("artifact_subscriptions")
                for (int i = 0; i < subscriptions.size(); i++) {
                    def sub = subscriptions.getJsonObject(i)
                    def appUuid = sub.getString("app_uuid")
                    def artifactQualifiedName = sub.getString("artifact_qualified_name")

                    SourcePortal.getPortals(appUuid, artifactQualifiedName).each {
                        if (it.interface.tracesView.artifactTraceResult) {
                            vertx.eventBus().send(UPDATE_DISPLAYED_TRACES, it.interface.tracesView.artifactTraceResult)
                        }
                    }
                }
            }
        })
        vertx.eventBus().consumer(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.address, {
            handleArtifactTraceResult(it.body() as ArtifactTraceResult)
        })

        //update displayed traces with new prepared traces
        vertx.eventBus().consumer(UPDATE_DISPLAYED_TRACES, {
            def artifactTraceResult = it.body() as ArtifactTraceResult
            SourcePortal.getPortals(artifactTraceResult.appUuid(), artifactTraceResult.artifactQualifiedName()).each {
                if (!pluginAvailable
                        || artifactTraceResult.artifactQualifiedName() == it.interface.viewingPortalArtifact) {
                    vertx.eventBus().publish(it.appUuid + "-" + it.portalId + "-DisplayTraces",
                            new JsonObject(Json.encode(artifactTraceResult)))
                    log.info("Displayed traces for artifact: " + artifactTraceResult.artifactQualifiedName()
                            + " - Trace size: " + artifactTraceResult.traces().size())
                }
            }
        })

        //user viewing portal under new artifact
        vertx.eventBus().consumer(PortalViewTracker.CHANGED_PORTAL_ARTIFACT, {
            def portal = SourcePortal.getPortal(JsonObject.mapFrom(it.body()).getInteger("portal_id"))
            vertx.eventBus().send(portal.appUuid + "-" + portal.portalId + "-ClearTraceStack", new JsonObject())
        })

        //populate with latest traces from cache (if avail) on switch to traces
        vertx.eventBus().consumer(PortalViewTracker.OPENED_PORTAL, {
            def portal = SourcePortal.getPortal(JsonObject.mapFrom(it.body()).getInteger("portal_id"))
            if (portal.interface.viewingPortalArtifact != null) {
                def representation = portal.interface.tracesView
                if (representation.innerTrace) {
                    def innerTraceStackInfo = InnerTraceStackInfo.builder()
                            .innerLevel(representation.innerLevel)
                            .traceStack(representation.innerTraceStack).build()
                    vertx.eventBus().publish("DisplayInnerTraceStack", new JsonObject(
                            Json.encode(innerTraceStackInfo)))
                    log.info("Displayed inner trace stack. Stack size: " + representation.innerTraceStack.size())
                }
                if (representation.artifactTraceResult != null) {
                    vertx.eventBus().send(UPDATE_DISPLAYED_TRACES, representation.artifactTraceResult)
                }
            }
        })

        //user clicked into trace stack
        vertx.eventBus().consumer(CLICKED_DISPLAY_TRACE_STACK, { messageHandler ->
            def request = messageHandler.body() as JsonObject
            log.debug("Displaying trace stack: " + request)

            vertx.eventBus().send(GET_TRACE_STACK, request, {
                if (it.failed()) {
                    it.cause().printStackTrace()
                    log.error("Failed to display trace stack", it.cause())
                } else {
                    def array = it.result().body() as JsonArray
                    messageHandler.reply(array)
                    log.info("Displayed trace stack for id: " + request.getString("trace_id") + " - Stack size: " + array.size())
                }
            })
        })

        vertx.eventBus().consumer(CLICKED_GO_BACK_TO_TRACES, {
            def portalId = (it.body() as JsonObject).getInteger("portal_id")
            def representation = SourcePortal.getPortal(portalId).interface.tracesView
            if (representation.rootArtifactQualifiedName == null) {
                representation.innerTrace = false
                representation.innerLevel = 0
            } else {
                vertx.eventBus().send("NavigateToArtifact",
                        new JsonObject().put("portal_id", portalId)
                                .put("artifact_qualified_name", representation.rootArtifactQualifiedName)
                )
            }
        })

        //user clicked into span
        vertx.eventBus().consumer(CLICKED_DISPLAY_SPAN_INFO, { messageHandler ->
            def spanInfoRequest = messageHandler.body() as JsonObject
            log.debug("Displaying span info: " + spanInfoRequest)

            def portalId = spanInfoRequest.getInteger("portal_id")
            def portal = SourcePortal.getPortal(portalId)
            def representation = portal.interface.tracesView
            def traceStack
            if (representation.innerTrace) {
                traceStack = representation.innerTraceStack
            } else {
                traceStack = representation.getTraceStack(spanInfoRequest.getString("trace_id"))
            }

            for (int i = 0; i < traceStack.size(); i++) {
                def span = traceStack.getJsonObject(i).getJsonObject("span")
                if (span.getInteger("span_id") == spanInfoRequest.getInteger("span_id")) {
                    def spanArtifactQualifiedName = span.getString("artifact_qualified_name")
                    if (spanArtifactQualifiedName == null
                            || spanArtifactQualifiedName == portal.interface.viewingPortalArtifact
                            || !pluginAvailable) {
                        messageHandler.reply(span)
                        log.info("Displayed trace span info: " + span)
                    } else {
                        messageHandler.reply(span)
                        log.info("Displayed trace span info: " + span)
//                        vertx.eventBus().send("CanNavigateToArtifact", spanArtifactQualifiedName, {
//                            if (it.succeeded() && it.result().body() == true) {
//                                def spanStackQuery = TraceSpanStackQuery.builder()
//                                        .oneLevelDeep(true).followExit(true)
//                                        .segmentId(span.getString("segment_id"))
//                                        .spanId(span.getLong("span_id"))
//                                        .traceId(spanInfoRequest.getString("trace_id")).build()
//
//                                //todo: cache
//                                coreClient.getTraceSpans(SourcePortalConfig.current.appUuid,
//                                        portal.portalUI.viewingPortalArtifact, spanStackQuery, {
//                                    if (it.failed()) {
//                                        log.error("Failed to get trace spans", it.cause())
//                                    } else {
//                                        def queryResult = it.result()
//                                        def innerLevel = representation.innerLevel + 1
//                                        representationCache.putIfAbsent(SourcePortalConfig.current.appUuid + "-" + spanArtifactQualifiedName, new TracesTabRepresentation())
//                                        representation = representationCache.get(SourcePortalConfig.current.appUuid + "-" + spanArtifactQualifiedName)
//
//                                        if (span.getString("type") == "Exit"
//                                                && queryResult.traceSpans().get(0).type() == "Entry") {
//                                            innerLevel = 0
//                                        } else {
//                                            representation.rootArtifactQualifiedName = portal.portalUI.viewingPortalArtifact
//                                        }
//                                        representation.innerTrace = true
//                                        representation.innerLevel = innerLevel
//                                        representation.innerTraceStack = handleTraceStack(
//                                                SourcePortalConfig.current.appUuid, portal.portalUI.viewingPortalArtifact, queryResult)
//                                        vertx.eventBus().send("NavigateToArtifact", spanArtifactQualifiedName)
//                                    }
//                                })
//                            } else {
//                                messageHandler.reply(span)
//                                log.info("Displayed trace span info: " + span)
//                            }
//                        })
                    }
                }
            }
        })

        //query core for trace stack (or get from cache)
        vertx.eventBus().consumer(GET_TRACE_STACK, { messageHandler ->
            def timer = PortalBootstrap.portalMetrics.timer(GET_TRACE_STACK)
            def context = timer.time()
            def request = messageHandler.body() as JsonObject
            def portalId = request.getInteger("portal_id")
            def appUuid = request.getString("app_uuid")
            def artifactQualifiedName = request.getString("artifact_qualified_name")
            def globalTraceId = request.getString("trace_id")
            log.trace("Getting trace spans. Artifact qualified name: %s - Trace id: %s",
                    artifactQualifiedName, globalTraceId)

            def portal = SourcePortal.getPortal(portalId)
            def representation = portal.interface.tracesView
            def traceStack = representation.getTraceStack(globalTraceId)
            if (traceStack != null) {
                log.trace("Got trace spans: $globalTraceId from cache - Stack size: " + traceStack.size())
                messageHandler.reply(traceStack)
                context.stop()
            } else {
                def traceStackQuery = TraceSpanStackQuery.builder()
                        .oneLevelDeep(true)
                        .traceId(globalTraceId).build()
                coreClient.getTraceSpans(appUuid, artifactQualifiedName, traceStackQuery, {
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

        vertx.eventBus().consumer(PortalViewTracker.UPDATED_METRIC_TIME_FRAME, {
            if (pluginAvailable) {
                def portalId = (it.body() as JsonObject).getInteger("portal_id")
                def portal = SourcePortal.getPortal(portalId)
                if (portal.interface.viewingPortalArtifact == null) {
                    return
                }

                //subscribe (re-subscribe) to traces
                def request = ArtifactTraceSubscribeRequest.builder()
                        .appUuid(portal.appUuid)
                        .artifactQualifiedName(portal.interface.viewingPortalArtifact)
                        .addOrderTypes(TraceOrderType.LATEST_TRACES, TraceOrderType.SLOWEST_TRACES)
                        .build()
                coreClient.subscribeToArtifact(request, {
                    if (it.succeeded()) {
                        log.info("Successfully subscribed to traces with request: " + request)
                    } else {
                        log.error("Failed to subscribe to artifact traces", it.cause())
                    }
                })
            } else {
                //subscribe (re-subscribe) to traces
                def subscriptions = config().getJsonArray("artifact_subscriptions")
                for (int i = 0; i < subscriptions.size(); i++) {
                    def sub = subscriptions.getJsonObject(i)
                    def request = ArtifactTraceSubscribeRequest.builder()
                            .appUuid(sub.getString("app_uuid"))
                            .artifactQualifiedName(sub.getString("artifact_qualified_name"))
                            .addOrderTypes(TraceOrderType.LATEST_TRACES, TraceOrderType.SLOWEST_TRACES)
                            .build()
                    coreClient.subscribeToArtifact(request, {
                        if (it.succeeded()) {
                            log.info("Successfully subscribed to traces with request: " + request)
                        } else {
                            log.error("Failed to subscribe to artifact traces", it.cause())
                        }
                    })
                }
            }
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void handleArtifactTraceResult(ArtifactTraceResult artifactTraceResult) {
        def traces = new ArrayList<Trace>()
        artifactTraceResult.traces().each {
            traces.add(it.withPrettyDuration(humanReadableDuration(Duration.ofMillis(it.duration()))))
        }
        artifactTraceResult = artifactTraceResult.withTraces(traces)
                .withArtifactSimpleName(removePackageAndClassName(removePackageNames(artifactTraceResult.artifactQualifiedName())))

        SourcePortal.getPortals(artifactTraceResult.appUuid(), artifactTraceResult.artifactQualifiedName()).each {
            def representation = it.interface.tracesView
            representation.cacheArtifactTraceResult(artifactTraceResult)

            if (!pluginAvailable || it.interface.viewingPortalArtifact == artifactTraceResult.artifactQualifiedName()) {
                vertx.eventBus().send(UPDATE_DISPLAYED_TRACES, artifactTraceResult)
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
