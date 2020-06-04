package com.sourceplusplus.core.integration.apm.skywalking

import com.sourceplusplus.api.model.artifact.SourceArtifactStatus
import com.sourceplusplus.api.model.trace.Trace
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.api.model.trace.TraceSpan
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.integration.apm.APMIntegrationConfig
import com.sourceplusplus.core.storage.CoreConfig
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.annotations.NotNull

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_STATUS_UPDATED
import static com.sourceplusplus.core.integration.apm.APMIntegrationConfig.SourceService

/**
 * todo: this
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SkywalkingFailingArtifacts extends AbstractVerticle {

    private final SkywalkingIntegration skywalking
    private final ArtifactAPI artifactAPI
    private final APMIntegrationConfig integrationConfig

    SkywalkingFailingArtifacts(SkywalkingIntegration skywalking, ArtifactAPI artifactAPI) {
        this.skywalking = skywalking
        this.artifactAPI = artifactAPI
        this.integrationConfig = CoreConfig.INSTANCE.apmIntegrationConfig
    }

    @Override
    void start(Promise<Void> startFuture) throws Exception {
        searchForFailingArtifacts(startFuture)
        vertx.setPeriodic(TimeUnit.SECONDS.toMillis(config().getJsonObject("config")
                .getInteger("failing_artifact_detection_interval_seconds")), {
            searchForFailingArtifacts({
                if (it.failed()) {
                    it.cause().printStackTrace()
                    log.error("Failed to search for failing artifacts", it.cause())
                }
            })
        })
    }

    @Override
    void stop() throws Exception {
        log.info("{} stopped", getClass().getSimpleName())
    }

    void searchForFailingArtifacts(Handler<AsyncResult<Void>> handler) {
        determineSourceServices({
            if (it.succeeded()) {
                doThing(it.result(), handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void doThing(@NotNull Set<SourceService> sourceServices, @NotNull Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def sourceService : sourceServices) {
            def future = Promise.promise()
            futures.add(future.future())

            determineActiveServiceInstances(sourceService, {
                if (it.succeeded()) {
                    doThing2(sourceService, it.result(), future)
                } else {
                    future.fail(it.cause())
                }
            })
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void doThing2(@NotNull SourceService sourceService, @NotNull JsonArray serviceInstances,
                          @NotNull Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (int i = 0; i < serviceInstances.size(); i++) {
            def future = Promise.promise()
            futures.add(future.future())

            def serviceInstance = serviceInstances.getJsonObject(i)
            def traceQuery = TraceQuery.builder()
                    .systemRequest(true)
                    .serviceId(sourceService.id)
                    .traceState("ERROR")
                    .durationStart(Instant.now().minus(140, ChronoUnit.MINUTES)) //todo:
                    .durationStop(Instant.now())
                    .durationStep("SECOND").build()
            skywalking.getTraces(traceQuery, {
                if (it.succeeded()) {
                    analyzeFailingTraces(sourceService, serviceInstance, it.result().traces(), handler)
                } else {
                    future.fail(it.cause())
                }
            })
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void determineSourceServices(@NotNull Handler<AsyncResult<Set<SourceService>>> handler) {
        def searchServiceStartTime
        if (integrationConfig.failedArtifactTracker.latestSearchedService == null) {
            searchServiceStartTime = Instant.now()
        } else {
            searchServiceStartTime = integrationConfig.failedArtifactTracker.latestSearchedService
        }
        def searchServiceEndTime = Instant.now()

        skywalking.getAllServices(searchServiceStartTime, searchServiceEndTime, "MINUTE", {
            if (it.succeeded()) {
                integrationConfig.failedArtifactTracker.latestSearchedService = searchServiceEndTime
                for (int i = 0; i < it.result().size(); i++) {
                    def service = it.result().getJsonObject(i)
                    def serviceId = service.getString("key")
                    def appUuid = service.getString("label") //todo: verify is app-uuid
                    integrationConfig.addSourceService(new SourceService(serviceId, appUuid))
                }
                handler.handle(Future.succeededFuture(integrationConfig.sourceServices))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void determineActiveServiceInstances(@NotNull SourceService sourceService,
                                                 @NotNull Handler<AsyncResult<JsonArray>> handler) {
        def searchServiceStartTime
        if (integrationConfig.failedArtifactTracker.latestSearchedServiceInstance == null) {
            searchServiceStartTime = Instant.now()
        } else {
            searchServiceStartTime = integrationConfig.failedArtifactTracker.latestSearchedServiceInstance
        }
        def searchServiceEndTime = Instant.now()

        skywalking.getServiceInstances(searchServiceStartTime, searchServiceEndTime, "MINUTE", sourceService.id, {
            if (it.succeeded()) {
                integrationConfig.failedArtifactTracker.latestSearchedServiceInstance = searchServiceEndTime
                handler.handle(Future.succeededFuture(it.result()))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void analyzeFailingTraces(@NotNull SourceService sourceService, @NotNull JsonObject serviceInstance,
                                      @NotNull List<Trace> traces, @NotNull Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def trace : traces) {
            trace.traceIds().each {
                def future = Promise.promise()
                skywalking.getTraceStack(null, it, {
                    if (it.succeeded()) {
                        processFailingTraceStack(sourceService, serviceInstance, it.result().traceSpans(), future)
                    } else {
                        future.fail(it.cause())
                    }
                })
                futures.add(future)
            }
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void processFailingTraceStack(@NotNull SourceService sourceService, @NotNull JsonObject serviceInstance,
                                          @NotNull List<TraceSpan> failingTraceSpans,
                                          @NotNull Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def failingSpan : failingTraceSpans) {
            if (!failingSpan.error) {
                continue
            }
            def future = Promise.promise()
            futures.add(future.future())

            def appUuid = failingSpan.serviceCode()
            def endpointName = failingSpan.endpointName()
            artifactAPI.findSourceArtifact(appUuid, endpointName, {
                if (it.succeeded()) {
                    if (it.result().isPresent()) {
                        def artifact = it.result().get()
                        def status
                        if (artifact.status() == null) {
                            status = SourceArtifactStatus.builder()
                                    .latestFailedTraceSpan(failingSpan)
                                    .build()
                        } else {
                            if (artifact.status().latestFailedTraceSpan() != null) {
                                if (artifact.status().latestFailedTraceSpan().traceId() != failingSpan.traceId()) {
                                    status = artifact.status()
                                            .withLatestFailedTraceSpan(failingSpan)
                                }
                            } else {
                                status = artifact.status()
                                        .withLatestFailedTraceSpan(failingSpan)
                            }
                        }

                        if (status != null) {
                            artifactAPI.createOrUpdateSourceArtifact(artifact.withStatus(status), {
                                if (it.succeeded()) {
                                    vertx.eventBus().publish(ARTIFACT_STATUS_UPDATED.address,
                                            new JsonObject(Json.encode(it.result())))
                                    future.complete()
                                } else {
                                    future.fail(it.cause())
                                }
                            })
                        } else {
                            future.complete()
                        }
                    } else {
                        future.complete()
                    }
                } else {
                    future.fail(it.cause())
                }
            })
        }
        CompositeFuture.all(futures).onComplete(handler)
    }
}
