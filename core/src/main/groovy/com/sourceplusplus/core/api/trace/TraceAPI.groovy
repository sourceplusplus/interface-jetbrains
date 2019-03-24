package com.sourceplusplus.core.api.trace

import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.error.SourceAPIErrors
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import com.sourceplusplus.core.api.trace.track.TraceSubscriptionTracker
import com.sourceplusplus.core.integration.skywalking.SkywalkingIntegration
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.shareddata.SharedData
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class TraceAPI extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final SharedData sharedData
    private final Router baseRouter
    private final ArtifactAPI artifactAPI
    private final SkywalkingIntegration skywalkingIntegration

    TraceAPI(SharedData sharedData, Router baseRouter,
             ArtifactAPI artifactAPI, SkywalkingIntegration skywalkingIntegration) {
        this.sharedData = Objects.requireNonNull(sharedData)
        this.baseRouter = Objects.requireNonNull(baseRouter)
        this.artifactAPI = Objects.requireNonNull(artifactAPI)
        this.skywalkingIntegration = Objects.requireNonNull(skywalkingIntegration)
    }

    @Override
    void start() throws Exception {
        baseRouter.put("/applications/:appUuid/artifacts/:artifactQualifiedName/traces/subscribe")
                .handler(this.&subscribeToArtifactRoute)
        baseRouter.put("/applications/:appUuid/artifacts/:artifactQualifiedName/traces/unsubscribe")
                .handler(this.&unsubscribeToArtifactRoute)
        baseRouter.get("/applications/:appUuid/artifacts/:artifactQualifiedName/traces").handler(this.&getTracesRoute)
        baseRouter.get("/applications/:appUuid/artifacts/:artifactQualifiedName/traces/:traceId/spans")
                .handler(this.&getTraceSpansRoute)
        vertx.deployVerticle(new TraceSubscriptionTracker(artifactAPI, this))
        log.info("{} started", getClass().getSimpleName())
    }

    private void unsubscribeToArtifactRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"))
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        ArtifactTraceUnsubscribeRequest request
        try {
            request = Json.decodeValue(routingContext.getBodyAsString(), ArtifactTraceUnsubscribeRequest.class)
        } catch (all) {
            all.printStackTrace()
            routingContext.response().setStatusCode(500)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }
        request = request.withAppUuid(appUuid).withArtifactQualifiedName(artifactQualifiedName)

        vertx.eventBus().send(ArtifactSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT, request, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200).end()
            } else {
                routingContext.response().setStatusCode(500)
                        .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            }
        })
    }

    private void subscribeToArtifactRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"))
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        ArtifactTraceSubscribeRequest request
        try {
            request = Json.decodeValue(routingContext.getBodyAsString(), ArtifactTraceSubscribeRequest.class)
        } catch (all) {
            all.printStackTrace()
            routingContext.response().setStatusCode(500)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }
        request = request.withAppUuid(appUuid).withArtifactQualifiedName(artifactQualifiedName)

        vertx.eventBus().send(ArtifactSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT, request, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200).end()
            } else {
                routingContext.response().setStatusCode(500)
                        .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            }
        })
    }

    private void getTracesRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"))
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        def traceQueryBuilder = TraceQuery.builder()
                .artifactQualifiedName(artifactQualifiedName)
        def orderType = routingContext.request().getParam("orderType")
        if (orderType != null) {
            //todo: dynamic
            traceQueryBuilder.durationStart(Instant.now().minus(14, ChronoUnit.MINUTES))
                    .durationStop(Instant.now())
                    .durationStep("SECOND")
        } else {
            def queryDurationStart = Instant.parse(routingContext.request().getParam("durationStart"))
            def queryDurationStop = Instant.parse(routingContext.request().getParam("durationStop"))
            def queryDurationStep = routingContext.request().getParam("durationStep")
            traceQueryBuilder.durationStart(queryDurationStart)
                    .durationStop(queryDurationStop)
                    .durationStep(queryDurationStep)
        }

        getTraces(appUuid, traceQueryBuilder.build(), {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    private void getTraceSpansRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"))
        def traceId = routingContext.request().getParam("traceId")
        if (!appUuid || !artifactQualifiedName || !traceId) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }
        def oneLevelDeep = routingContext.request().getParam("oneLevelDeep") as boolean
        def followExit = routingContext.request().getParam("followExit") as boolean
        def segmentId = routingContext.request().getParam("segmentId")
        def spanId = routingContext.request().getParam("spanId")

        def traceSpanQuery = TraceSpanStackQuery.builder()
                .followExit(followExit)
                .oneLevelDeep(oneLevelDeep)
                .traceId(traceId)
        if (segmentId) {
            traceSpanQuery.segmentId(segmentId)
        }
        if (spanId) {
            traceSpanQuery.spanId(spanId as long)
        }

        getTraceSpans(appUuid, artifactQualifiedName, traceSpanQuery.build(), {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void getTraces(String appUuid, TraceQuery traceQuery, Handler<AsyncResult<TraceQueryResult>> handler) {
        if (traceQuery.endpointId() != null || traceQuery.endpointName()) {
            //todo: query without getting artifact config
            throw new UnsupportedOperationException("todo: this")
        }

        artifactAPI.getSourceArtifactConfig(appUuid, traceQuery.artifactQualifiedName(), {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    def artifactConfig = it.result().get()
                    if (artifactConfig.endpoint() || artifactConfig.endpointIds()) {
                        if (artifactConfig.endpointIds()) {
                            def futures = new ArrayList<Future>()
                            artifactConfig.endpointIds().each {
                                traceQuery = traceQuery.withEndpointId(it)
                                def fut = Future.future()
                                skywalkingIntegration.getSkywalkingTraces(traceQuery, fut.completer())
                                futures.add(fut)
                            }
                            CompositeFuture.all(futures).setHandler({
                                if (it.succeeded()) {
                                    List<Trace> totalTraces = []
                                    (it.result().list() as List<TraceQueryResult>).each {
                                        totalTraces.addAll(it.traces())
                                    }
                                    totalTraces.sort({ it.start() })

                                    def finalResult = TraceQueryResult.builder()
                                            .addAllTraces(totalTraces)
                                            .total(totalTraces.size()).build()
                                    handler.handle(Future.succeededFuture(finalResult))
                                } else {
                                    handler.handle(Future.failedFuture(it.cause()))
                                }
                            })
                        } else {
                            log.warn("Could not find endpoint id for endpoint. Artifact qualified name: " + traceQuery.artifactQualifiedName())
                            handler.handle(Future.succeededFuture(TraceQueryResult.builder().total(0).build()))
                        }
                    } else {
                        log.debug("No traces exists for artifact. Artifact qualified name: " + traceQuery.artifactQualifiedName())
                        handler.handle(Future.succeededFuture(TraceQueryResult.builder().total(0).build()))
                    }
                } else {
                    log.warn("Could not find artifact config. Artifact qualified name: " + traceQuery.artifactQualifiedName())
                    handler.handle(Future.succeededFuture(TraceQueryResult.builder().total(0).build()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    void getTraceSpans(String appUuid, String artifactQualifiedName, TraceSpanStackQuery traceSpanQuery,
                       Handler<AsyncResult<TraceSpanStackQueryResult>> handler) {
        if (traceSpanQuery.oneLevelDeep()) {
            artifactAPI.getSourceArtifact(appUuid, artifactQualifiedName, {
                if (it.succeeded()) {
                    if (it.result().isPresent()) {
                        skywalkingIntegration.getSkywalkingTraceStack(appUuid, it.result().get(), traceSpanQuery, handler)
                    } else {
                        skywalkingIntegration.getSkywalkingTraceStack(appUuid, null, traceSpanQuery, handler)
                    }
                } else {
                    handler.handle(Future.failedFuture(it.cause()))
                }
            })
        } else {
            skywalkingIntegration.getSkywalkingTraceStack(appUuid, null, traceSpanQuery, handler)
        }
    }
}
