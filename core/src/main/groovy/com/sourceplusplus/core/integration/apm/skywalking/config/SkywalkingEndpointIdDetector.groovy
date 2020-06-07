package com.sourceplusplus.core.integration.apm.skywalking.config

import com.google.common.collect.Sets
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.api.model.trace.TraceSpan
import com.sourceplusplus.core.api.application.ApplicationAPI
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.integration.apm.APMIntegrationConfig
import com.sourceplusplus.core.integration.apm.skywalking.SkywalkingFailingArtifacts
import com.sourceplusplus.core.integration.apm.skywalking.SkywalkingIntegration
import com.sourceplusplus.core.storage.CoreConfig
import com.sourceplusplus.core.storage.SourceStorage
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED
import static com.sourceplusplus.core.integration.apm.APMIntegrationConfig.SourceService
import static com.sourceplusplus.core.integration.apm.skywalking.SkywalkingIntegration.UNKNOWN_COMPONENT

/**
 * Used to match artifacts to SkyWalking endpoint ids.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SkywalkingEndpointIdDetector extends AbstractVerticle {

    public static final String SEARCH_FOR_NEW_ENDPOINTS = "SearchForNewEndpoints"

    private static final Map<String, Long[]> endpointCheckBackoff = new ConcurrentHashMap<>()
    private final SkywalkingIntegration skywalking
    private final ApplicationAPI applicationAPI
    private final ArtifactAPI artifactAPI
    private final SourceStorage storage
    private final APMIntegrationConfig integrationConfig

    SkywalkingEndpointIdDetector(SkywalkingIntegration skywalking, ApplicationAPI applicationAPI,
                                 ArtifactAPI artifactAPI, SourceStorage storage) {
        this.skywalking = Objects.requireNonNull(skywalking)
        this.applicationAPI = Objects.requireNonNull(applicationAPI)
        this.artifactAPI = Objects.requireNonNull(artifactAPI)
        this.storage = Objects.requireNonNull(storage)
        this.integrationConfig = CoreConfig.INSTANCE.apmIntegrationConfig
    }

    @Override
    void start(Promise<Void> startFuture) throws Exception {
        vertx.deployVerticle(new SkywalkingFailingArtifacts(skywalking, applicationAPI, artifactAPI, storage),
                new DeploymentOptions().setConfig(config()))

        vertx.eventBus().consumer(ARTIFACT_CONFIG_UPDATED.address, { message ->
            def artifact = Json.decodeValue((message.body() as JsonObject).toString(), SourceArtifact.class)
            if (artifact.config().endpointName() != null) {
                Set<String> endpointIds = new HashSet<>()
                if (artifact.config().endpointName().contains("{")) {
                    handleDynamicEndpointName(artifact.config(), endpointIds)
                } else {
                    def endpointId = vertx.sharedData().getLocalMap("skywalking_endpoints")
                            .get(artifact.config().endpointName()) as String
                    if (endpointId && (!artifact.config().endpointIds()
                            || !artifact.config().endpointIds().contains(endpointId))) {
                        endpointIds.add(endpointId)
                    }
                }

                if (endpointIds != null && !endpointIds.isEmpty()) {
                    if (artifact.config().endpointIds() == null) {
                        addEndpointIdsToArtifactConfig(artifact, endpointIds, {
                            if (it.failed()) {
                                log.error("Failed to add endpoint id(s) to artifact config", it.cause())
                            }
                        })
                    } else {
                        addEndpointIdsToArtifactConfig(artifact, artifact.config().endpointIds() + endpointIds, {
                            if (it.failed()) {
                                log.error("Failed to add endpoint id(s) to artifact config", it.cause())
                            }
                        })
                    }
                }
            }
        })

        searchForNewEndpoints(startFuture)
        vertx.setPeriodic(TimeUnit.SECONDS.toMillis(config().getJsonObject("config")
                .getInteger("endpoint_detection_interval_seconds")), {
            searchForNewEndpoints({
                if (it.failed()) {
                    it.cause().printStackTrace()
                    log.error("Failed to search for new endpoints", it.cause())
                }
            })
        })
        vertx.eventBus().consumer(SEARCH_FOR_NEW_ENDPOINTS, { handler ->
            searchForNewEndpoints({
                if (it.succeeded()) {
                    handler.reply(true)
                } else {
                    it.cause().printStackTrace()
                    log.error("Failed to search for new endpoints", it.cause())
                    handler.fail(500, it.cause().message)
                }
            })
        })
        log.info("{} started", getClass().getSimpleName())
    }

    @Override
    void stop() throws Exception {
        log.info("{} stopped", getClass().getSimpleName())
    }

    private void handleDynamicEndpointName(SourceArtifactConfig config, HashSet<String> endpointIds) {
        //using wildcard, iterate saved endpoints
        def replacedWildcard = config.endpointName().replaceAll(Pattern.compile('(\\{.+\\})'), '.+')
        def configMap = vertx.sharedData().getLocalMap("skywalking_endpoints")
        for (def it in configMap) {
            def endpointName = it.key as String
            if (config.moduleName() && endpointName.startsWith("/" + config.moduleName())) {
                endpointName = endpointName.substring(config.moduleName().length() + 1)
            }

            if (endpointName ==~ replacedWildcard) {
                if (config.endpointIds() == null || !config.endpointIds().contains(it.value)) {
                    endpointIds.add(it.value as String)
                }
                vertx.sharedData().getLocalMap("skywalking_endpoint_alias").put(it.key, config.endpointName())
            }
        }
    }

    private void searchForNewEndpoints(Handler<AsyncResult<Void>> handler) {
        log.debug("Searching for new SkyWalking service endpoints")
        determineSourceServices({
            if (it.succeeded()) {
                def futures = []
                for (def service : it.result()) {
                    def fut = Promise.promise()
                    futures.add(fut)
                    skywalking.getServiceEndpoints(service.id, {
                        if (it.succeeded()) {
                            def searchEndpoints = new JsonArray()
                            for (int z = 0; z < it.result().size(); z++) {
                                //get random endpoint
                                def endpoint = it.result().getJsonObject(ThreadLocalRandom.current().nextInt(it.result().size()))
                                def endpointId = endpoint.getString("key")
                                endpointCheckBackoff.putIfAbsent(endpointId, [0, Instant.now().toEpochMilli()] as Long[])

                                def expireTime = endpointCheckBackoff.get(endpointId)[1]
                                if (expireTime <= Instant.now().toEpochMilli()) {
                                    searchEndpoints.add(endpoint)
                                    def values = endpointCheckBackoff.get(endpointId)
                                    values[0]++
                                    if (values[0] >= 240) values[0] = 240 //max 1 hour wait
                                    values[1] = Instant.now().plus(values[0] * 15, ChronoUnit.SECONDS).toEpochMilli()
                                    endpointCheckBackoff.put(endpointId, values)
                                } else {
                                    log.debug("Ignoring endpoint: $endpointId - Till: " + Instant.ofEpochMilli(expireTime))
                                }

                                //search max of 10 endpoints at a time
                                if (searchEndpoints.size() >= 10) {
                                    break
                                }
                            }
                            if (searchEndpoints.size() > 0) {
                                searchServiceEndpoints(service.appUuid, searchEndpoints, fut)
                            } else {
                                fut.complete()
                            }
                        } else {
                            fut.fail(it.cause())
                        }
                    })
                }
                CompositeFuture.all(futures).onComplete(handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void determineSourceServices(Handler<AsyncResult<Set<SourceService>>> handler) {
        def searchServiceStartTime
        if (integrationConfig.endpointDetection.latestSearchedService == null) {
            searchServiceStartTime = Instant.now()
        } else {
            searchServiceStartTime = integrationConfig.endpointDetection.latestSearchedService
        }
        def searchServiceEndTime = Instant.now()

        skywalking.getAllServices(searchServiceStartTime, searchServiceEndTime, "MINUTE", {
            if (it.succeeded()) {
                integrationConfig.endpointDetection.latestSearchedService = searchServiceEndTime

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
                                integrationConfig.addSourceService(new SourceService(serviceId, appUuid))
                            }
                            promise.complete()
                        } else {
                            promise.fail(it.cause())
                        }
                    })
                }

                CompositeFuture.all(futures).onComplete({
                    if (it.succeeded()) {
                        handler.handle(Future.succeededFuture(integrationConfig.sourceServices))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void searchServiceEndpoints(String appUuid, JsonArray serviceEndpoints,
                                        Handler<AsyncResult<Void>> handler) {
        log.info("Searching service endpoints. App UUID: $appUuid - Endpoints: $serviceEndpoints")
        def futures = []
        for (int z = 0; z < serviceEndpoints.size(); z++) {
            def serviceEndpoint = serviceEndpoints.getJsonObject(z)
            String endpointName = serviceEndpoint.getString("label")
            String endpointId = serviceEndpoint.getString("key")
            vertx.sharedData().getLocalMap("skywalking_endpoints").put(endpointName, endpointId)

            def fut = Promise.promise()
            futures.add(fut)
            artifactAPI.getSourceArtifact(appUuid, endpointName, {
                if (it.succeeded()) {
                    if (it.result().isPresent()) {
                        def artifact = it.result().get()
                        if (artifact.config().endpointIds() == null
                                || !artifact.config().endpointIds().contains(endpointId)) {
                            addEndpointIdsToArtifactConfig(artifact, Sets.newHashSet(endpointId), fut)
                        } else {
                            fut.complete()
                        }
                    } else {
                        searchServiceName(appUuid, endpointName, endpointId, {
                            if (it.succeeded()) {
                                if (it.result().isPresent()) {
                                    fut.complete()
                                } else {
                                    searchServiceId(appUuid, endpointId, endpointName, fut)
                                }
                            } else {
                                fut.fail(it.cause())
                            }
                        })
                    }
                } else {
                    fut.fail(it.cause())
                }
            })
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void searchServiceId(String appUuid, String endpointId, String endpointName,
                                 Handler<AsyncResult<Void>> handler) {
        log.info("Searching service id. App UUID: $appUuid - Endpoint id: $endpointId - Endpoint name: $endpointName")
        artifactAPI.getSourceArtifactByEndpointId(appUuid, endpointId, {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    def sourceArtifact = it.result().get()
                    if (sourceArtifact.config() && sourceArtifact.config().endpointName()
                            && sourceArtifact.config().endpointName().contains("{")) {
                        //dynamically named endpoint; save aliases to memory
                        vertx.sharedData().getLocalMap("skywalking_endpoint_alias")
                                .put(endpointName, sourceArtifact.config().endpointName())
                    }
                    handler.handle(Future.succeededFuture())
                } else {
                    vertx.sharedData().getLocalMap("skywalking_pending_endpoints")
                            .put(endpointName, endpointId)
                    analyzeEndpointTraces(appUuid, endpointId, handler)
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void searchServiceName(String appUuid, String endpointName, String endpointId,
                                   Handler<AsyncResult<Optional<SourceArtifactConfig>>> handler) {
        log.info("Searching service name. App UUID: $appUuid - Endpoint id: $endpointId - Endpoint name: $endpointName")
        artifactAPI.getSourceArtifactByEndpointName(appUuid, endpointName, {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    def sourceArtifact = it.result().get()
                    if (!sourceArtifact.config().endpointIds()
                            || !sourceArtifact.config().endpointIds().contains(endpointId)) {
                        addEndpointIdsToArtifactConfig(sourceArtifact, Sets.newHashSet(endpointId), {
                            if (it.succeeded()) {
                                handler.handle(Future.succeededFuture(Optional.of(it.result())))
                            } else {
                                handler.handle(Future.failedFuture(it.cause()))
                            }
                        })
                    } else {
                        handler.handle(Future.succeededFuture(Optional.empty()))
                    }
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void analyzeEndpointTraces(String appUuid, String endpointId, Handler<AsyncResult<Void>> handler) {
        log.info("Analayzing endpoint traces. App UUID: $appUuid - Endpoint id: $endpointId")
        //todo: should be a limit in this query and should look further back than 15 minutes
        //todo: related to #186
        def traceQuery = TraceQuery.builder()
                .systemRequest(true)
                .appUuid(appUuid)
                .endpointId(endpointId)
                .durationStart(Instant.now().minus(14, ChronoUnit.MINUTES))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        skywalking.getTraces(traceQuery, {
            if (it.succeeded()) {
                def futures = []
                //todo: likely don't need to check all traces
                it.result().traces().each {
                    it.traceIds().each {
                        def fut = Promise.promise()
                        futures.add(fut)
                        skywalking.getTraceStack(appUuid, it, {
                            if (it.succeeded()) {
                                def spans = it.result().traceSpans()
                                if (spans && spans.size() > 1) {
                                    analyzeEndpointSpans(appUuid, spans, fut)
                                } else {
                                    fut.complete()
                                }
                            } else {
                                fut.handle(Future.failedFuture(it.cause()))
                            }
                        })
                    }
                }
                CompositeFuture.all(futures).onComplete(handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void analyzeEndpointSpans(String appUuid, List<TraceSpan> spans, Handler<AsyncResult<Void>> handler) {
        log.info("Analayzing endpoint spans. App UUID: $appUuid - Span size: " + spans.size())
        def futures = []
        for (int i = 0; i < spans.size(); i++) {
            def span = spans.get(i)
            if (span.endpointName() && !span.artifactQualifiedName()) {
                def endpointId = vertx.sharedData().getLocalMap("skywalking_pending_endpoints")
                        .get(span.endpointName()) as String
                if (endpointId) {
                    if (!span.component().isEmpty() && UNKNOWN_COMPONENT != span.component()) {
                        while (!span.component().isEmpty() && UNKNOWN_COMPONENT != span.component()
                                && i + 1 < spans.size()) {
                            span = spans.get(++i) //skip entry component spans
                        }
                        --i //step back so nextSpan is span after entry component
                    }

                    if (i == 0 && span.type() == "Entry" && span.component() == UNKNOWN_COMPONENT) {
                        def fut = Promise.promise()
                        futures.add(fut)
                        lookupSpanArtifact(span, appUuid, endpointId, fut)
                    } else if (i + 1 < spans.size()) {
                        def fut = Promise.promise()
                        futures.add(fut)
                        lookupSpanArtifact(spans.get(i + 1), appUuid, endpointId, fut)
                    }
                }
            }
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void lookupSpanArtifact(TraceSpan span, String appUuid, String endpointId,
                                    Handler<AsyncResult<Void>> handler) {
        if (span.artifactQualifiedName() || span.endpointName()) {
            def possibleArtifactQualifiedName = span.artifactQualifiedName()
            if (!possibleArtifactQualifiedName) {
                possibleArtifactQualifiedName = span.endpointName()
            }

            artifactAPI.getSourceArtifact(appUuid, possibleArtifactQualifiedName, {
                if (it.succeeded()) {
                    if (it.result().isPresent()) {
                        def artifact = it.result().get()
                        addEndpointIdsToArtifactConfig(artifact, Sets.newHashSet(endpointId), {
                            if (it.succeeded()) {
                                handler.handle(Future.succeededFuture())
                            } else {
                                handler.handle(Future.failedFuture(it.cause()))
                            }
                        })
                    } else {
                        handler.handle(Future.succeededFuture())
                    }
                } else {
                    handler.handle(Future.failedFuture(it.cause()))
                }
            })
        } else {
            handler.handle(Future.succeededFuture())
        }
    }

    private void addEndpointIdsToArtifactConfig(SourceArtifact artifact, Set<String> endpointIds,
                                                Handler<AsyncResult<SourceArtifactConfig>> handler) {
        log.info(String.format("Adding endpoints %s to artifact config for artifact: %s",
                endpointIds, artifact.artifactQualifiedName()))
        artifactAPI.createOrUpdateSourceArtifactConfig(artifact.appUuid(), artifact.artifactQualifiedName(),
                artifact.config().withEndpoint(true).withEndpointIds(artifact.config().endpointIds() == null
                        ? endpointIds : artifact.config().endpointIds() + endpointIds), handler)
    }
}
