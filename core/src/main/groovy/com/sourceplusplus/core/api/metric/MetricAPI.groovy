package com.sourceplusplus.core.api.metric

import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.error.SourceAPIErrors
import com.sourceplusplus.api.model.metric.ArtifactMetricQuery
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetricSubscribeRequest
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import com.sourceplusplus.core.api.metric.track.MetricSubscriptionTracker
import com.sourceplusplus.core.integration.skywalking.SkywalkingIntegration
import com.sourceplusplus.core.storage.ElasticsearchDAO
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.shareddata.SharedData
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * todo: description
 *
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class MetricAPI extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final SharedData sharedData
    private final Router baseRouter
    private final ArtifactAPI artifactAPI
    private final ElasticsearchDAO elasticsearch
    private final SkywalkingIntegration skywalkingIntegration

    MetricAPI(SharedData sharedData, Router baseRouter, ArtifactAPI artifactAPI, ElasticsearchDAO elasticsearch,
              SkywalkingIntegration skywalkingIntegration) {
        this.sharedData = Objects.requireNonNull(sharedData)
        this.baseRouter = Objects.requireNonNull(baseRouter)
        this.artifactAPI = Objects.requireNonNull(artifactAPI)
        this.elasticsearch = Objects.requireNonNull(elasticsearch)
        this.skywalkingIntegration = Objects.requireNonNull(skywalkingIntegration)
    }

    @Override
    void start() throws Exception {
        baseRouter.put("/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/subscribe")
                .handler(this.&subscribeToArtifactRoute)
        baseRouter.put("/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/unsubscribe")
                .handler(this.&unsubscribeToArtifactRoute)
        baseRouter.get("/applications/:appUuid/artifacts/:artifactQualifiedName/metrics")
                .handler(this.&getArtifactMetricsRoute)

        vertx.deployVerticle(new MetricSubscriptionTracker(artifactAPI, this))
        log.info("MetricAPI started")
    }

    private void unsubscribeToArtifactRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"))
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        ArtifactMetricUnsubscribeRequest request
        try {
            request = Json.decodeValue(routingContext.getBodyAsString(), ArtifactMetricUnsubscribeRequest.class)
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

        ArtifactMetricSubscribeRequest request
        try {
            request = Json.decodeValue(routingContext.getBodyAsString(), ArtifactMetricSubscribeRequest.class)
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

    private void getArtifactMetricsRoute(RoutingContext routingContext) {
//        def appUuid = routingContext.request().getParam("appUuid")
//        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"))
//        if (appUuid == null || appUuid.isAllWhitespace()
//                || artifactQualifiedName == null || artifactQualifiedName.isAllWhitespace()) {
//            routingContext.response().setStatusCode(400)
//                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
//            return
//        }
//        def durationStart = Instant.parse(routingContext.request().getParam("durationStart"))
//        def durationStop = Instant.parse(routingContext.request().getParam("durationStop"))
//        def durationStep = routingContext.request().getParam("durationStep")
//
//        def metricType = MetricType.Throughput_Average //todo: dynamic
//        def orderType = QueryTimeFrame.LAST_15_MINUTES //todo: dynamic
//        def timeFramedMetricType = TimeFramedMetricType.builder().metricType(metricType).orderType(orderType).build()
//        def metricQuery = ArtifactMetricQuery.builder()
//                .timeFramedMetricType(timeFramedMetricType)
//                .appUuid(appUuid)
//                .artifactQualifiedName(artifactQualifiedName)
//                .start(durationStart)
//                .stop(durationStop)
//                .step(durationStep).build()
//        getArtifactMetrics(metricQuery, {
//            if (it.succeeded()) {
//                routingContext.response().setStatusCode(200)
//                        .end(Json.encode(it.result()))
//            } else {
//                routingContext.response().setStatusCode(400)
//                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
//            }
//        })
    }

    void getArtifactMetrics(ArtifactMetricQuery metricQuery, Handler<AsyncResult<ArtifactMetricResult>> handler) {
        artifactAPI.getSourceArtifact(metricQuery.appUuid(), metricQuery.artifactQualifiedName(), {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    def artifactConfig = it.result().get().config()
                    def endpointIds = artifactConfig?.endpointIds()
                    if (endpointIds != null && !endpointIds.isEmpty()) {
                        def endpointId = endpointIds[0] //todo: not only use first endpoint id
                        skywalkingIntegration.getEndpointMetrics(endpointId, metricQuery, handler)
                    } else {
                        log.warn("Could not find endpoint id for artifact: " + metricQuery.artifactQualifiedName())
                        //todo: doesn't complete handler
                    }
                }
            }
        })
    }
}
