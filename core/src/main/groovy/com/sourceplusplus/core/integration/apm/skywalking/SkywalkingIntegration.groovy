package com.sourceplusplus.core.integration.apm.skywalking

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.metric.ArtifactMetricQuery
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetrics
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.integration.apm.APMIntegration
import com.sourceplusplus.core.integration.apm.skywalking.config.SkywalkingEndpointIdDetector
import com.sourceplusplus.core.storage.SourceStorage
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SkywalkingIntegration extends APMIntegration {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static final String GET_ALL_SERVICES = Resources.toString(Resources.getResource(
            "query/skywalking/get_all_services.graphql"), Charsets.UTF_8)
    private static final String GET_SERVICE_ENDPOINTS = Resources.toString(Resources.getResource(
            "query/skywalking/get_service_endpoints.graphql"), Charsets.UTF_8)
    private static final String GET_ENDPOINT_METRICS = Resources.toString(Resources.getResource(
            "query/skywalking/get_endpoint_metrics.graphql"), Charsets.UTF_8)
    private static final String GET_LATEST_TRACES = Resources.toString(Resources.getResource(
            "query/skywalking/get_latest_traces.graphql"), Charsets.UTF_8)
    private static final String GET_SLOWEST_TRACES = Resources.toString(Resources.getResource(
            "query/skywalking/get_slowest_traces.graphql"), Charsets.UTF_8)
    private static final String GET_TRACE_STACK = Resources.toString(Resources.getResource(
            "query/skywalking/get_trace_stack.graphql"), Charsets.UTF_8)
    private final ArtifactAPI artifactAPI
    private final SourceStorage storage
    private DateTimeFormatter DATE_TIME_FORMATTER_MINUTES
    private DateTimeFormatter DATE_TIME_FORMATTER_SECONDS
    private String skywalkingOAPHost
    private int skywalkingOAPPort
    private WebClient webClient

    SkywalkingIntegration(ArtifactAPI artifactAPI, SourceStorage storage) {
        this.artifactAPI = Objects.requireNonNull(artifactAPI)
        this.storage = Objects.requireNonNull(storage)
    }

    @Override
    void start(Future<Void> startFuture) throws Exception {
        def timezone = config().getJsonObject("config").getString("timezone")
        if (timezone) {
            DATE_TIME_FORMATTER_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm")
                    .withZone(ZoneId.of(timezone))
            DATE_TIME_FORMATTER_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss")
                    .withZone(ZoneId.of(timezone))
        } else {
            DATE_TIME_FORMATTER_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm")
                    .withZone(ZoneId.systemDefault())
            DATE_TIME_FORMATTER_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss")
                    .withZone(ZoneId.systemDefault())
        }
        skywalkingOAPHost = Objects.requireNonNull(config().getJsonObject("connection").getString("host"))
        skywalkingOAPPort = Objects.requireNonNull(config().getJsonObject("connection").getInteger("port"))

        webClient = WebClient.create(vertx)
        vertx.deployVerticle(new SkywalkingEndpointIdDetector(this, artifactAPI),
                new DeploymentOptions().setConfig(config()), {
            if (it.succeeded()) {
                log.info("SkywalkingIntegration started")
                startFuture.complete()
            } else {
                startFuture.fail(it.cause())
            }
        })
    }

    void getAllServices(Instant start, Instant end, String step, Handler<AsyncResult<JsonArray>> handler) {
        log.info("Getting all SkyWalking services. Start: " + start + " - End: " + end)
        def graphqlQuery = new JsonObject()
        graphqlQuery.put("query", GET_ALL_SERVICES
                .replace('$durationStart', DATE_TIME_FORMATTER_SECONDS.format(start))
                .replace('$durationEnd', DATE_TIME_FORMATTER_SECONDS.format(end))
                .replace('$durationStep', step)
        )

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                def result = new JsonObject(it.result().bodyAsString())
                        .getJsonObject("data").getJsonArray("getAllServices")
                log.info("Got all SkyWalking services: " + result)
                handler.handle(Future.succeededFuture(result))
            }
        })
    }

    void getServiceEndpoints(String serviceId, Handler<AsyncResult<JsonArray>> handler) {
        log.info("Getting SkyWalking service endpoints for service id: " + serviceId)
        def graphqlQuery = new JsonObject()
        graphqlQuery.put("query", GET_SERVICE_ENDPOINTS
                .replace('$serviceId', serviceId)
        )

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                def result = new JsonObject(it.result().bodyAsString())
                        .getJsonObject("data").getJsonArray("searchEndpoint")
                log.info("Got SkyWalking service endpoints: " + result)
                handler.handle(Future.succeededFuture(result))
            }
        })
    }

    void getEndpointMetrics(String endpointId, ArtifactMetricQuery metricQuery,
                            Handler<AsyncResult<ArtifactMetricResult>> handler) {
        log.debug("Getting SkyWalking endpoint metrics: " + Objects.requireNonNull(metricQuery))
        def graphqlQuery = new JsonObject()
        if ("second".equalsIgnoreCase(metricQuery.step())) {
            graphqlQuery.put("query", GET_ENDPOINT_METRICS
                    .replace('$endpointId', endpointId)
                    .replace('$durationStart', DATE_TIME_FORMATTER_SECONDS.format(metricQuery.start()))
                    .replace('$durationEnd', DATE_TIME_FORMATTER_SECONDS.format(metricQuery.stop()))
                    .replace('$durationStep', metricQuery.step())
            )
        } else if ("minute".equalsIgnoreCase(metricQuery.step())) {
            graphqlQuery.put("query", GET_ENDPOINT_METRICS
                    .replace('$endpointId', endpointId)
                    .replace('$durationStart', DATE_TIME_FORMATTER_MINUTES.format(metricQuery.start()))
                    .replace('$durationEnd', DATE_TIME_FORMATTER_MINUTES.format(metricQuery.stop()))
                    .replace('$durationStep', metricQuery.step())
            )
        }
        //todo: make this use batch endpoint ids

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                def data = it.result().bodyAsJsonObject().getJsonObject("data")
                def artifactMetrics = new ArrayList<ArtifactMetrics>()
                metricQuery.metricTypes().each {
                    switch (it) {
                        case MetricType.Throughput_Average:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonObject("getEndpointThroughputTrend")
                                    .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_Average:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonObject("getEndpointResponseTimeTrend")
                                    .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ServiceLevelAgreement_Average:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonObject("getEndpointSLATrend")
                                    .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_99Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonObject("getEndpointResponseTimeP99Trend")
                                    .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_95Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonObject("getEndpointResponseTimeP95Trend")
                                    .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_90Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonObject("getEndpointResponseTimeP90Trend")
                                    .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_75Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonObject("getEndpointResponseTimeP75Trend")
                                    .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_50Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonObject("getEndpointResponseTimeP50Trend")
                                    .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        default:
                            throw new UnsupportedOperationException("Invalid metric type: " + it)
                    }
                }

                def artifactMetricResult = ArtifactMetricResult.builder()
                        .appUuid(metricQuery.appUuid())
                        .artifactQualifiedName(metricQuery.artifactQualifiedName())
                        .timeFrame(metricQuery.timeFrame())
                        .start(metricQuery.start())
                        .stop(metricQuery.stop())
                        .step(metricQuery.step())
                        .artifactMetrics(artifactMetrics).build()
                log.debug("Got SkyWalking endpoint metrics: " + artifactMetricResult)
                handler.handle(Future.succeededFuture(artifactMetricResult))
            }
        })
    }

    @Override
    void getTraces(TraceQuery traceQuery, Handler<AsyncResult<TraceQueryResult>> handler) {
        log.info("Getting SkyWalking traces: " + Objects.requireNonNull(traceQuery))
        def graphqlQuery = new JsonObject()
        if (traceQuery.orderType() == TraceOrderType.LATEST_TRACES) {
            graphqlQuery.put("query", GET_LATEST_TRACES
                    .replace('$endpointId', traceQuery.endpointId())
                    .replace('$queryDurationStart', DATE_TIME_FORMATTER_SECONDS.format(traceQuery.durationStart()))
                    .replace('$queryDurationEnd', DATE_TIME_FORMATTER_SECONDS.format(traceQuery.durationStop()))
                    .replace('$queryDurationStep', traceQuery.durationStep())
            )
        } else {
            graphqlQuery.put("query", GET_SLOWEST_TRACES
                    .replace('$endpointId', traceQuery.endpointId())
                    .replace('$queryDurationStart', DATE_TIME_FORMATTER_SECONDS.format(traceQuery.durationStart()))
                    .replace('$queryDurationEnd', DATE_TIME_FORMATTER_SECONDS.format(traceQuery.durationStop()))
                    .replace('$queryDurationStep', traceQuery.durationStep())
            )
        }

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                JsonObject result = new JsonObject(it.result().bodyAsString())
                        .getJsonObject("data").getJsonObject("queryBasicTraces")
                def traceList = new ArrayList<Trace>()
                def traces = result.getJsonArray("traces")
                for (int i = 0; i < traces.size(); i++) {
                    traceList.add(Json.decodeValue(traces.getJsonObject(i).toString(), Trace.class))
                }
                if (traceQuery.orderType() == TraceOrderType.LATEST_TRACES) {
                    traceList.sort({ it.start() })
                } else if (traceQuery.orderType() == TraceOrderType.SLOWEST_TRACES) {
                    traceList.sort({ it.duration() })
                }

                def traceQueryResult = TraceQueryResult.builder()
                        .traces(traceList)
                        .total(result.getInteger("total"))
                        .build()

                log.info("Got SkyWalking traces. Traces: {} - Total: {}",
                        traceQueryResult.traces().size(), traceQueryResult.total())
                handler.handle(Future.succeededFuture(traceQueryResult))
            }
        })
    }

    @Override
    void getTraceStack(String appUuid, SourceArtifact artifact, TraceSpanStackQuery spanQuery,
                       Handler<AsyncResult<TraceSpanStackQueryResult>> handler) {
        log.info("Getting SkyWalking trace spans: " + Objects.requireNonNull(spanQuery))
        def graphqlQuery = new JsonObject()
        graphqlQuery.put("query", GET_TRACE_STACK
                .replace('$globalTraceId', spanQuery.traceId())
        )

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                def spanList = new ArrayList<TraceSpan>()
                def data = it.result().bodyAsJsonObject().getJsonObject("data").getJsonObject("queryTrace")
                if (data != null) {
                    def parentSpanId = -1
                    def prevSpanId = -1
                    def exitSegmentId = null
                    long exitSpanId = -1

                    def stack = data.getJsonArray("spans")
                    for (int i = 0; i < stack.size(); i++) {
                        def traceSpan = Json.decodeValue(stack.getJsonObject(i).toString(), TraceSpan.class)

                        if (artifact && spanQuery.oneLevelDeep()) {
                            if (parentSpanId != -1 && prevSpanId != -1 && traceSpan.spanId() < prevSpanId) {
                                break
                            } else {
                                prevSpanId = traceSpan.spanId()
                            }

                            if (parentSpanId == -1 && spanQuery.segmentId() != null) {
                                if ((spanQuery.segmentId() == traceSpan.segmentId()
                                        && spanQuery.spanId() == traceSpan.spanId()) || exitSegmentId != null) {
                                    if (traceSpan.type() == "Exit" && exitSegmentId == null && spanQuery.followExit()) {
                                        exitSegmentId = traceSpan.segmentId()
                                        exitSpanId = traceSpan.spanId()
                                    } else if (exitSegmentId != null && spanQuery.followExit()) {
                                        boolean foundEntry = false
                                        def refs = traceSpan.refs()
                                        refs.each {
                                            if (exitSegmentId == it.parentSegmentId()
                                                    && exitSpanId == it.parentSpanId()
                                                    && it.type() == "CROSS_PROCESS") {
                                                foundEntry = true
                                            }
                                        }
                                        if (foundEntry) {
                                            spanList.add(traceSpan)
                                            parentSpanId = traceSpan.spanId()
                                        }
                                    } else {
                                        spanList.add(traceSpan)
                                        parentSpanId = traceSpan.spanId()
                                    }
                                }
                            } else if (parentSpanId == -1 && (traceSpan.endpointName() == artifact.artifactQualifiedName()
                                    || (artifact.config() && traceSpan.endpointName() == artifact.config().endpointName()))) {
                                if (!spanList.isEmpty() || traceSpan.type() != "Exit") {
                                    spanList.add(traceSpan)
                                    parentSpanId = traceSpan.spanId()
                                }
                            } else if (parentSpanId != -1 && traceSpan.parentSpanId() == parentSpanId) {
                                spanList.add(traceSpan)
                            }
                        } else {
                            spanList.add(traceSpan)
                        }
                    }
                }

                def futures = new ArrayList<Future>()
                spanList.each {
                    def fut = Future.future()
                    String endpointName = it.endpointName()
                    def endpointId = vertx.sharedData().getLocalMap("skywalking_endpoints")
                            .get(endpointName) as String
                    if (endpointId) {
                        artifactAPI.getSourceArtifactByEndpointId(appUuid, endpointId, fut.completer())
                    } else {
                        artifactAPI.getSourceArtifactByEndpointName(appUuid, endpointName, fut.completer())
                    }
                    futures.add(fut)
                }
                CompositeFuture.all(futures).setHandler({
                    if (it.succeeded()) {
                        def results = it.result().list()
                        def traceSpanList = new ArrayList<TraceSpan>()
                        for (int i = 0; i < spanList.size(); i++) {
                            def sourceArtifact = results.get(i) as Optional<SourceArtifact>
                            if (sourceArtifact.isPresent()) {
                                traceSpanList.add(spanList.get(i).withArtifactQualifiedName(
                                        sourceArtifact.get().artifactQualifiedName()))
                            } else {
                                traceSpanList.add(spanList.get(i))
                            }
                        }

                        def traceSpanQueryResult = TraceSpanStackQueryResult.builder()
                                .traceSpans(traceSpanList)
                                .total(spanList.size())
                                .build()
                        log.info("Got SkyWalking trace spans: " + traceSpanQueryResult.total())
                        handler.handle(Future.succeededFuture(traceSpanQueryResult))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            }
        })
    }
}
