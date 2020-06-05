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
import io.vertx.core.json.JsonArray

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

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

    private void doThing(Set<SourceService> sourceServices, Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def sourceService : sourceServices) {
            def future = Promise.promise()
            futures.add(future.future())

            determineActiveServiceInstances(sourceService, {
                if (it.succeeded()) {
                    //todo: result not used
                    doThing2(sourceService, future)
                } else {
                    future.fail(it.cause())
                }
            })
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void doThing2(SourceService sourceService,
                          Handler<AsyncResult<Void>> handler) {
        def traceQuery = TraceQuery.builder()
                .systemRequest(true)
                .serviceId(sourceService.id)
                .traceState("ERROR")
                .durationStart(Instant.now().minus(140, ChronoUnit.MINUTES)) //todo:
                .durationStop(Instant.now())
                .durationStep("SECOND").build()
        skywalking.getTraces(traceQuery, {
            if (it.succeeded()) {
                analyzeFailingTraces(it.result().traces(), handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void determineSourceServices(Handler<AsyncResult<Set<SourceService>>> handler) {
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

    private void determineActiveServiceInstances(SourceService sourceService,
                                                 Handler<AsyncResult<JsonArray>> handler) {
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
                def serviceInstances = it.result()
                Set<String> serviceInstanceIds = new HashSet<>()
                for (int i = 0; i < serviceInstances.size(); i++) {
                    serviceInstanceIds.add(serviceInstances.getJsonObject(i).getString("label"))
                }

                artifactAPI.updateFailingArtifacts(sourceService.appUuid, serviceInstanceIds, {
                    if (it.succeeded()) {
                        handler.handle(Future.succeededFuture(serviceInstances))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void analyzeFailingTraces(List<Trace> traces, Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def trace : traces) {
            trace.traceIds().each {
                def future = Promise.promise()
                skywalking.getTraceStack(null, it, {
                    if (it.succeeded()) {
                        processFailingTraceStack(it.result().traceSpans(), future)
                    } else {
                        future.fail(it.cause())
                    }
                })
                futures.add(future)
            }
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void processFailingTraceStack(List<TraceSpan> failingTraceSpans,
                                          Handler<AsyncResult<Void>> handler) {
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
                                    .latestFailedSpan(failingSpan)
                                    .build()
                        } else {
                            if (artifact.status().latestFailedSpan() != null) {
                                if (artifact.status().latestFailedSpan().traceId() != failingSpan.traceId()) {
                                    status = artifact.status()
                                            .withLatestFailedSpan(failingSpan)
                                }
                            } else {
                                status = artifact.status()
                                        .withLatestFailedSpan(failingSpan)
                            }
                        }

                        if (status != null) {
                            artifactAPI.createOrUpdateSourceArtifact(artifact.withStatus(status), future)
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
