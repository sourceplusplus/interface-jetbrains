package com.sourceplusplus.core.integration.apm.skywalking

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.metric.ArtifactMetricQuery
import com.sourceplusplus.api.model.metric.ArtifactMetricResult
import com.sourceplusplus.api.model.metric.ArtifactMetrics
import com.sourceplusplus.api.model.metric.MetricType
import com.sourceplusplus.api.model.trace.*
import com.sourceplusplus.core.api.application.ApplicationAPI
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.integration.apm.APMIntegration
import com.sourceplusplus.core.integration.apm.skywalking.config.SkywalkingEndpointIdDetector
import com.sourceplusplus.core.integration.apm.skywalking.status.SkywalkingFailingArtifacts
import com.sourceplusplus.core.storage.CoreConfig
import com.sourceplusplus.core.storage.SourceStorage
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import static com.sourceplusplus.api.util.ArtifactNameUtils.*
import static com.sourceplusplus.api.model.trace.TraceOrderType.*
import static com.sourceplusplus.core.SourceCoreServer.RESOURCE_LOADER
import static com.sourceplusplus.core.integration.apm.APMIntegrationConfig.SourceService

/**
 * Represents integration with the Apache SkyWalking APM.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SkywalkingIntegration extends APMIntegration {

    public static final String UNKNOWN_COMPONENT = "Unknown"

    private static final String GET_ALL_SERVICES = Resources.toString(RESOURCE_LOADER.getResource(
            "query/skywalking/get_all_services.graphql"), Charsets.UTF_8)
    private static final String GET_SERVICE_INSTANCES = Resources.toString(RESOURCE_LOADER.getResource(
            "query/skywalking/get_service_instances.graphql"), Charsets.UTF_8)
    private static final String GET_SERVICE_ENDPOINTS = Resources.toString(RESOURCE_LOADER.getResource(
            "query/skywalking/get_service_endpoints.graphql"), Charsets.UTF_8)
    private static final String GET_ENDPOINT_METRICS = Resources.toString(RESOURCE_LOADER.getResource(
            "query/skywalking/get_endpoint_metrics.graphql"), Charsets.UTF_8)
    private static final String QUERY_BASIC_TRACES = Resources.toString(RESOURCE_LOADER.getResource(
            "query/skywalking/query_basic_traces.graphql"), Charsets.UTF_8)
    private static final String GET_TRACE_STACK = Resources.toString(RESOURCE_LOADER.getResource(
            "query/skywalking/get_trace_stack.graphql"), Charsets.UTF_8)
    private final ApplicationAPI applicationAPI
    private final ArtifactAPI artifactAPI
    private final SourceStorage storage
    private DateTimeFormatter DATE_TIME_FORMATTER_MINUTES
    private DateTimeFormatter DATE_TIME_FORMATTER_SECONDS
    private String skywalkingOAPHost
    private int skywalkingOAPPort
    private WebClient webClient
    private int serviceDetectionDelay

    SkywalkingIntegration(ApplicationAPI applicationAPI, ArtifactAPI artifactAPI, SourceStorage storage) {
        this.applicationAPI = Objects.requireNonNull(applicationAPI)
        this.artifactAPI = Objects.requireNonNull(artifactAPI)
        this.storage = Objects.requireNonNull(storage)
    }

    @Override
    void start(Promise<Void> startFuture) throws Exception {
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

        def restHost = Objects.requireNonNull(config().getJsonObject("connections").getJsonObject("REST"))
        skywalkingOAPHost = Objects.requireNonNull(restHost.getString("host"))
        skywalkingOAPPort = Objects.requireNonNull(restHost.getInteger("port"))
        webClient = WebClient.create(vertx)

        serviceDetectionDelay = config().getJsonObject("config").getInteger("service_detection_delay_seconds")

        def deploymentConfig = new DeploymentOptions().setConfig(config())
        def failingArtifactsPromise = Promise.promise().future()
        def endpointIdDetectorPromise = Promise.promise().future()
        vertx.deployVerticle(new SkywalkingFailingArtifacts(this, applicationAPI, artifactAPI, storage),
                deploymentConfig, failingArtifactsPromise)
        vertx.deployVerticle(new SkywalkingEndpointIdDetector(this, applicationAPI, artifactAPI, storage),
                deploymentConfig, endpointIdDetectorPromise)
        CompositeFuture.all(failingArtifactsPromise, endpointIdDetectorPromise).onComplete({
            if (it.succeeded()) {
                log.info("SkywalkingIntegration started")
                startFuture.complete()
            } else {
                startFuture.fail(it.cause())
            }
        })
    }

    @Override
    void stop() throws Exception {
        log.info("{} stopped", getClass().getSimpleName())
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getAllServices(Instant start, Instant end, String step,
                        Handler<AsyncResult<JsonArray>> handler) {
        log.debug("Getting all SkyWalking services. Start: " + start + " - End: " + end)
        def graphqlQuery = new JsonObject()
        if ("second".equalsIgnoreCase(step)) {
            graphqlQuery.put("query", GET_ALL_SERVICES
                    .replace('$durationStart', DATE_TIME_FORMATTER_SECONDS.format(start))
                    .replace('$durationEnd', DATE_TIME_FORMATTER_SECONDS.format(end))
                    .replace('$durationStep', step)
            )
        } else {
            graphqlQuery.put("query", GET_ALL_SERVICES
                    .replace('$durationStart', DATE_TIME_FORMATTER_MINUTES.format(start))
                    .replace('$durationEnd', DATE_TIME_FORMATTER_MINUTES.format(end))
                    .replace('$durationStep', step)
            )
        }

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                def result = it.result().bodyAsJsonObject()
                        .getJsonObject("data").getJsonArray("getAllServices")
                log.debug("Got all SkyWalking services: " + result)
                handler.handle(Future.succeededFuture(result))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getServiceInstances(Instant start, Instant end, String step, String serviceId,
                             Handler<AsyncResult<JsonArray>> handler) {
        log.debug("Getting all SkyWalking service instances. Start: $start - End: $end - Service id: $serviceId")
        def graphqlQuery = new JsonObject()
        if ("second".equalsIgnoreCase(step)) {
            graphqlQuery.put("query", GET_SERVICE_INSTANCES
                    .replace('$serviceId', serviceId)
                    .replace('$durationStart', DATE_TIME_FORMATTER_SECONDS.format(start))
                    .replace('$durationEnd', DATE_TIME_FORMATTER_SECONDS.format(end))
                    .replace('$durationStep', step)
            )
        } else {
            graphqlQuery.put("query", GET_SERVICE_INSTANCES
                    .replace('$serviceId', serviceId)
                    .replace('$durationStart', DATE_TIME_FORMATTER_MINUTES.format(start))
                    .replace('$durationEnd', DATE_TIME_FORMATTER_MINUTES.format(end))
                    .replace('$durationStep', step)
            )
        }

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                def result = it.result().bodyAsJsonObject()
                        .getJsonObject("data").getJsonArray("getServiceInstances")
                log.debug("Got all SkyWalking service instances: " + result)
                handler.handle(Future.succeededFuture(result))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getActiveServiceInstances(Handler<AsyncResult<JsonArray>> handler) {
        log.info("Getting all active SkyWalking service instances")
        def start = Instant.now(), end = Instant.now(), step = "MINUTE"
        getAllServices(start, end, "MINUTE", {
            if (it.succeeded()) {
                def futures = []
                for (int i = 0; i < it.result().size(); i++) {
                    def promise = Promise.promise()
                    futures.add(promise)

                    def service = it.result().getJsonObject(i)
                    getServiceInstances(start, end, step, service.getString("label"), promise)
                }

                CompositeFuture.all(futures).onComplete({
                    if (it.succeeded()) {
                        def resultArray = new JsonArray()
                        for (int i = 0; i < it.result().size(); i++) {
                            def serviceInstance = it.result().resultAt(i) as JsonArray
                            if (!serviceInstance.isEmpty()) {
                                resultArray.add(serviceInstance)
                            }
                        }
                        handler.handle(Future.succeededFuture(resultArray))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    void getServiceEndpoints(String serviceId, Handler<AsyncResult<JsonArray>> handler) {
        log.debug("Getting SkyWalking service endpoints for service id: " + serviceId)
        def graphqlQuery = new JsonObject()
        graphqlQuery.put("query", GET_SERVICE_ENDPOINTS
                .replace('$serviceId', serviceId)
        )

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                def result = it.result().bodyAsJsonObject()
                        .getJsonObject("data").getJsonArray("searchEndpoint")
                log.debug("Got SkyWalking service endpoints: " + result)
                handler.handle(Future.succeededFuture(result))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
                                    .values(data.getJsonArray("getEndpointResponseTimePercentileTrend").getJsonObject(4)
                                            .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_95Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonArray("getEndpointResponseTimePercentileTrend").getJsonObject(3)
                                            .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_90Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonArray("getEndpointResponseTimePercentileTrend").getJsonObject(2)
                                            .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_75Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonArray("getEndpointResponseTimePercentileTrend").getJsonObject(1)
                                            .getJsonArray("values").flatten { it.getInteger("value") } as List<Integer>)
                                    .build()
                            artifactMetrics.add(metrics)
                            break
                        case MetricType.ResponseTime_50Percentile:
                            def metrics = ArtifactMetrics.builder()
                                    .metricType(it)
                                    .values(data.getJsonArray("getEndpointResponseTimePercentileTrend").getJsonObject(0)
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

    /**
     * {@inheritDoc}
     */
    @Override
    void getTraces(TraceQuery traceQuery, Handler<AsyncResult<TraceQueryResult>> handler) {
        log.debug("Getting SkyWalking traces: " + Objects.requireNonNull(traceQuery))

        def serviceIdStr = "null"
        if (traceQuery.serviceId() != null) {
            serviceIdStr = '"' + traceQuery.serviceId() + '"'
        }
        def serviceInstanceIdStr = "null"
        if (traceQuery.serviceInstanceId() != null) {
            serviceInstanceIdStr = '"' + traceQuery.serviceInstanceId() + '"'
        }
        def endpointIdStr = "null"
        if (traceQuery.endpointId() != null) {
            endpointIdStr = '"' + traceQuery.endpointId() + '"'
        }
        def endpointNameStr = "null"
        if (traceQuery.artifactQualifiedName() != null) {
            endpointNameStr = '"' + traceQuery.artifactQualifiedName() + '"'
        } else if (traceQuery.endpointName() != null) {
            endpointNameStr = '"' + traceQuery.endpointName() + '"'
        }
        def queryOrder = traceQuery.orderType() == SLOWEST_TRACES ? "BY_DURATION" : "BY_START_TIME"
        def graphqlQuery = new JsonObject()
        graphqlQuery.put("query", QUERY_BASIC_TRACES
                .replace('$serviceId', serviceIdStr)
                .replace('$serviceInstanceId', serviceInstanceIdStr)
                .replace('$endpointId', endpointIdStr)
                .replace('$endpointName', endpointNameStr)
                .replace('$queryDurationStart', DATE_TIME_FORMATTER_SECONDS.format(traceQuery.durationStart()))
                .replace('$queryDurationEnd', DATE_TIME_FORMATTER_SECONDS.format(traceQuery.durationStop()))
                .replace('$queryDurationStep', traceQuery.durationStep())
                .replace('$traceState', traceQuery.traceState())
                .replace('$queryOrder', queryOrder)
                .replace('$pageSize', Integer.toString(traceQuery.pageSize())))

        webClient.post(skywalkingOAPPort, skywalkingOAPHost,
                "/graphql").sendJsonObject(graphqlQuery, {
            if (it.failed()) {
                handler.handle(Future.failedFuture(it.cause()))
            } else {
                def result = it.result().bodyAsJsonObject()
                        .getJsonObject("data").getJsonObject("queryBasicTraces")
                def traceList = new ArrayList<Trace>()
                def traces = result.getJsonArray("traces")
                for (int i = 0; i < traces.size(); i++) {
                    traceList.add(Json.decodeValue(traces.getJsonObject(i).toString(), Trace.class))
                }
                if (traceQuery.orderType() == LATEST_TRACES || traceQuery.orderType() == FAILED_TRACES) {
                    traceList.sort({ it.start() })
                } else if (traceQuery.orderType() == SLOWEST_TRACES) {
                    traceList.sort({ it.duration() })
                }

                def traceQueryResult = TraceQueryResult.builder()
                        .traces(traceList)
                        .total(result.getInteger("total"))
                        .build()

                def primarySelector = ""
                if (traceQuery.artifactQualifiedName() != null) {
                    primarySelector = "Artifact: ${getShortQualifiedFunctionName(traceQuery.artifactQualifiedName())} - "
                } else if (traceQuery.endpointId() != null) {
                    primarySelector = "Endpoint: ${traceQuery.endpointId()} - "
                } else if (traceQuery.endpointName() != null) {
                    primarySelector = "Endpoint: ${traceQuery.endpointName()} - "
                } else if (traceQuery.serviceInstanceId() != null) {
                    primarySelector = "Service instance id: ${traceQuery.serviceInstanceId()} - "
                } else if (traceQuery.serviceId() != null) {
                    primarySelector = "Service id: ${traceQuery.serviceId()} - "
                }
                log.info("Got SkyWalking traces. {}State: {} - Order: {} - Traces: {} - Total: {}",
                        primarySelector, traceQuery.traceState(), queryOrder,
                        traceQueryResult.traces().size(), traceQueryResult.total())

                handler.handle(Future.succeededFuture(traceQueryResult))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getTraceStack(String appUuid, String traceId, Handler<AsyncResult<TraceSpanStackQueryResult>> handler) {
        def query = TraceSpanStackQuery.builder()
                .systemRequest(true)
                .oneLevelDeep(false)
                .traceId(traceId).build()
        getTraceStack(appUuid, null, query, handler)
    }

    /**
     * {@inheritDoc}
     */
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
                ArrayList<TraceSpan> spanList = processTraceStack(
                        it.result().bodyAsJsonObject().getJsonObject("data").getJsonObject("queryTrace"),
                        spanQuery, artifact, vertx.sharedData().getLocalMap("skywalking_endpoints"))
                def futures = new ArrayList<Future>()
                spanList.each {
                    def fut = Promise.promise()
                    String endpointName = it.endpointName()
                    def endpointId = vertx.sharedData().getLocalMap("skywalking_endpoints")
                            .get(endpointName) as String
                    if (endpointId) {
                        artifactAPI.getSourceArtifactByEndpointId(appUuid, endpointId, fut)
                    } else {
                        artifactAPI.getSourceArtifactByEndpointName(appUuid, endpointName, fut)
                    }
                    futures.add(fut as Future)  //see: #130
                }
                CompositeFuture.all(futures).onComplete({
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

    void determineSourceServices(Handler<AsyncResult<Set<SourceService>>> handler) {
        def searchServiceStartTime
        def returnCached = false
        if (CoreConfig.INSTANCE.apmIntegrationConfig.latestSearchedService == null) {
            searchServiceStartTime = Instant.now()
        } else {
            searchServiceStartTime = CoreConfig.INSTANCE.apmIntegrationConfig.latestSearchedService
            returnCached = Instant.now().epochSecond - searchServiceStartTime.epochSecond <= serviceDetectionDelay
        }

        if (returnCached) {
            handler.handle(Future.succeededFuture(CoreConfig.INSTANCE.apmIntegrationConfig.sourceServices))
        } else {
            def searchServiceEndTime = Instant.now()
            getAllServices(searchServiceStartTime, searchServiceEndTime, "MINUTE", {
                if (it.succeeded()) {
                    CoreConfig.INSTANCE.apmIntegrationConfig.latestSearchedService = searchServiceEndTime

                    def futures = []
                    for (int i = 0; i < it.result().size(); i++) {
                        def service = it.result().getJsonObject(i)
                        def serviceId = service.getString("key")
                        def appUuid = service.getString("label")

                        //verify appUuid is valid
                        def promise = Promise.promise()
                        futures.add(promise)
                        applicationAPI.getApplication(appUuid, {
                            if (it.succeeded()) {
                                if (it.result().isPresent()) {
                                    CoreConfig.INSTANCE.apmIntegrationConfig.addSourceService(
                                            new SourceService(serviceId, appUuid))
                                }
                                promise.complete()
                            } else {
                                promise.fail(it.cause())
                            }
                        })
                    }

                    CompositeFuture.all(futures).onComplete({
                        if (it.succeeded()) {
                            handler.handle(Future.succeededFuture(CoreConfig.INSTANCE.apmIntegrationConfig.sourceServices))
                        } else {
                            handler.handle(Future.failedFuture(it.cause()))
                        }
                    })
                } else {
                    handler.handle(Future.failedFuture(it.cause()))
                }
            })
        }
    }

    static ArrayList<TraceSpan> processTraceStack(JsonObject traceStackData, TraceSpanStackQuery spanQuery,
                                                  SourceArtifact artifact, Map<String, String> skywalkingEndpoints) {
        def processedSpans = new ArrayList<TraceSpan>()
        if (traceStackData != null) {
            def stack = traceStackData.getJsonArray("spans")
            def segments = new HashMap<String, List<TraceSpan>>()
            for (int i = 0; i < stack.size(); i++) {
                def traceSpan = Json.decodeValue(stack.getJsonObject(i).toString(), TraceSpan.class)
                segments.putIfAbsent(traceSpan.segmentId(), new ArrayList<TraceSpan>())
                segments.get(traceSpan.segmentId()).add(traceSpan)
            }

            def segmentSpanChildStacks = new HashMap<String, HashSet<Long>>()
            def spanList = new ArrayList<TraceSpan>()
            for (def traceSpans : segments.values()) {
                def parentSpanId = -2L
                if (spanQuery.oneLevelDeep()) {
                    if (spanQuery.spanId() != null) {
                        parentSpanId = spanQuery.spanId()
                    } else {
                        def parentSpan = traceSpans.find {
                            isMatchingArtifact(it, artifact, skywalkingEndpoints)
                        }
                        if (parentSpan) {
                            parentSpanId = parentSpan.spanId()
                        } else {
                            continue
                        }
                    }
                }

                def orderedTraceSpans = traceSpans.sort { it.spanId() }.reverse()
                def childErrors = new HashSet<Long>()
                for (def traceSpan : orderedTraceSpans) {
                    if (parentSpanId != traceSpan.parentSpanId() && traceSpan.parentSpanId() >= 0) {
                        segmentSpanChildStacks.putIfAbsent(traceSpan.segmentId(), new HashSet<Long>())
                        segmentSpanChildStacks.get(traceSpan.segmentId()).add(traceSpan.parentSpanId())
                    }
                    if (traceSpan.isError()) {
                        childErrors.add(traceSpan.parentSpanId())
                    }

                    def processedSpan = traceSpan
                    if (childErrors.contains(traceSpan.spanId())) {
                        processedSpan = processedSpan.withIsChildError(true)
                        childErrors.add(traceSpan.parentSpanId())
                    }
                    if (spanQuery.oneLevelDeep() && traceSpan.spanId() != parentSpanId &&
                            traceSpan.parentSpanId() != parentSpanId) {
                        processedSpan = null
                    }

                    if (processedSpan != null) {
                        spanList.add(processedSpan)
                    }
                }
            }

            spanList.reverse().each {
                if (segmentSpanChildStacks.get(it.segmentId())?.contains(it.spanId())) {
                    processedSpans.add(it.withHasChildStack(true))
                } else {
                    processedSpans.add(it)
                }
            }
        }
        return processedSpans
    }

    private static boolean isMatchingArtifact(TraceSpan traceSpan, SourceArtifact artifact,
                                              Map<String, String> skywalkingEndpoints) {
        if (traceSpan.component() && traceSpan.component() != UNKNOWN_COMPONENT) {
            return false //skip entry component spans
        }

        def endpointId = skywalkingEndpoints.get(traceSpan.endpointName())
        if (endpointId && artifact.config().endpointIds() && artifact.config().endpointIds().contains(endpointId)) {
            return true
        } else {
            return traceSpan.endpointName() == artifact.artifactQualifiedName() ||
                    (artifact.config() && traceSpan.endpointName() == artifact.config().endpointName())
        }
    }
}
