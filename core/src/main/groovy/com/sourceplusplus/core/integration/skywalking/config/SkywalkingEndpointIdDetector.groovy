package com.sourceplusplus.core.integration.skywalking.config

import com.google.common.collect.Sets
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.api.model.trace.TraceSpan
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.integration.skywalking.SkywalkingIntegration
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED

/**
 * todo: description
 *
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class SkywalkingEndpointIdDetector extends AbstractVerticle {

    public static final String SEARCH_FOR_NEW_ENDPOINTS = "SearchForNewEndpoints"

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private final SkywalkingIntegration skywalking
    private final ArtifactAPI artifactAPI

    SkywalkingEndpointIdDetector(SkywalkingIntegration skywalking, ArtifactAPI artifactAPI) {
        this.skywalking = skywalking
        this.artifactAPI = artifactAPI
    }

    @Override
    void start() throws Exception {
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

        searchForNewEndpoints({
            if (it.failed()) {
                it.cause().printStackTrace()
                log.error("Failed to search for new endpoints", it.cause())
            }
        })
        vertx.setPeriodic(TimeUnit.SECONDS.toMillis(30), {
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
        skywalking.getAllServices(Instant.now().minus(7, ChronoUnit.DAYS), Instant.now(), "MINUTE", {
            if (it.succeeded()) {
                def futures = []
                def services = it.result()
                for (int i = 0; i < services.size(); i++) {
                    def service = it.result().getJsonObject(i)
                    def appUuid = service.getString("label")

                    def fut = Future.future()
                    futures.add(fut)
                    skywalking.getServiceEndpoints(service.getString("key"), {
                        if (it.succeeded()) {
                            searchServiceEndpoints(appUuid, it.result(), fut.completer())
                        } else {
                            fut.fail(it.cause())
                        }
                    })
                }
                CompositeFuture.all(futures).setHandler(handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void searchServiceEndpoints(String appUuid, JsonArray serviceEndpoints,
                                        Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (int z = 0; z < serviceEndpoints.size(); z++) {
            def serviceEndpoint = serviceEndpoints.getJsonObject(z)
            def endpointName = serviceEndpoint.getString("label")
            def endpointId = serviceEndpoint.getString("key")
            vertx.sharedData().getLocalMap("skywalking_endpoints").put(endpointName, endpointId)

            def fut = Future.future()
            futures.add(fut)
            artifactAPI.getSourceArtifact(appUuid, endpointName, {
                if (it.succeeded()) {
                    if (it.result().isPresent()) {
                        def artifact = it.result().get()
                        if (artifact.config() == null || artifact.config().endpointIds() == null
                                || !artifact.config().endpointIds().contains(endpointId)) {
                            addEndpointIdsToArtifactConfig(artifact, Sets.newHashSet(endpointId), fut.completer())
                        } else {
                            fut.complete()
                        }
                    } else {
                        searchServiceName(appUuid, endpointName, endpointId, {
                            if (it.succeeded()) {
                                if (it.result().isPresent()) {
                                    fut.complete()
                                } else {
                                    searchServiceId(appUuid, endpointId, endpointName, fut.completer())
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
        CompositeFuture.all(futures).setHandler(handler)
    }

    private void searchServiceId(String appUuid, String endpointId, String endpointName,
                                 Handler<AsyncResult<Void>> handler) {
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
        artifactAPI.getSourceArtifactByEndpointName(appUuid, endpointName, {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    def sourceArtifact = it.result().get()
                    if (!sourceArtifact.config() || !sourceArtifact.config().endpointIds()
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
        //todo: should be a limit in this query and should look further back than 15 minutes
        def traceQuery = TraceQuery.builder()
                .appUuid(appUuid)
                .endpointId(endpointId)
                .durationStart(Instant.now().minus(14, ChronoUnit.MINUTES))
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        skywalking.getSkywalkingTraces(traceQuery, {
            if (it.succeeded()) {
                def futures = []
                it.result().traces().each {
                    it.traceIds().each {
                        def fut = Future.future()
                        futures.add(fut)
                        skywalking.getSkywalkingTraceStack(appUuid, it, {
                            if (it.succeeded()) {
                                def spans = it.result().traceSpans()
                                if (spans && spans.size() > 1) {
                                    analyzeEndpointSpans(appUuid, spans, fut.completer())
                                } else {
                                    fut.complete()
                                }
                            } else {
                                fut.handle(Future.failedFuture(it.cause()))
                            }
                        })
                    }
                }
                CompositeFuture.all(futures).setHandler(handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void analyzeEndpointSpans(String appUuid, List<TraceSpan> spans, Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (int i = 0; i < spans.size(); i++) {
            def span = spans.get(i)
            if (span.endpointName() && !span.artifactQualifiedName()) {
                def endpointId = vertx.sharedData().getLocalMap("skywalking_pending_endpoints")
                        .get(span.endpointName()) as String
                if (endpointId) {
                    if (i + 1 < spans.size()) {
                        def nextSpan = spans.get(i + 1)
                        if (nextSpan.artifactQualifiedName()) {
                            def fut = Future.future()
                            futures.add(fut)
                            artifactAPI.getSourceArtifact(appUuid, nextSpan.artifactQualifiedName(), {
                                if (it.succeeded()) {
                                    if (it.result().isPresent()) {
                                        def artifact = it.result().get()
                                        if (artifact.config() && artifact.config().endpoint()) {
                                            addEndpointIdsToArtifactConfig(artifact, Sets.newHashSet(endpointId),
                                                    fut.completer())
                                        } else {
                                            fut.complete()
                                        }
                                    } else {
                                        fut.complete()
                                    }
                                } else {
                                    fut.fail(it.cause())
                                }
                            })
                        }
                    }
                }
            }
        }
        CompositeFuture.all(futures).setHandler(handler)
    }

    private void addEndpointIdsToArtifactConfig(SourceArtifact artifact, Set<String> endpointIds,
                                                Handler<AsyncResult<SourceArtifactConfig>> handler) {
        log.info(String.format("Adding endpoints %s to artifact config for artifact: %s",
                endpointIds, artifact.artifactQualifiedName()))
        if (artifact.config() == null) {
            artifact = artifact.withConfig(SourceArtifactConfig.builder().build())
        }
        artifactAPI.createOrUpdateSourceArtifactConfig(artifact.appUuid(), artifact.artifactQualifiedName(),
                artifact.config().withEndpointIds(artifact.config().endpointIds() == null
                        ? endpointIds : artifact.config().endpointIds() + endpointIds), handler)
    }
}
