package com.sourceplusplus.core.integration.apm.skywalking.status

import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactStatus
import com.sourceplusplus.api.model.trace.Trace
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.api.model.trace.TraceSpan
import com.sourceplusplus.core.api.application.ApplicationAPI
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.integration.apm.skywalking.SkywalkingIntegration
import com.sourceplusplus.core.storage.CoreConfig
import com.sourceplusplus.core.storage.SourceStorage
import groovy.util.logging.Slf4j
import io.vertx.core.*

import java.time.Instant
import java.util.concurrent.TimeUnit

import static com.sourceplusplus.core.integration.apm.APMIntegrationConfig.*

/**
 * Queries Apache SkyWalking for failing traces and correlates them to stored sources code artifacts.
 *
 * @version 0.3.0
 * @since 0.3.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SkywalkingFailingArtifacts extends AbstractVerticle {

    private final SkywalkingIntegration skywalking
    private final ApplicationAPI applicationAPI
    private final ArtifactAPI artifactAPI
    private final SourceStorage storage
    private final FailedArtifactTracker failedArtifactTracker

    SkywalkingFailingArtifacts(SkywalkingIntegration skywalking, ApplicationAPI applicationAPI,
                               ArtifactAPI artifactAPI, SourceStorage storage) {
        this.skywalking = Objects.requireNonNull(skywalking)
        this.applicationAPI = Objects.requireNonNull(applicationAPI)
        this.artifactAPI = Objects.requireNonNull(artifactAPI)
        this.storage = Objects.requireNonNull(storage)
        this.failedArtifactTracker = CoreConfig.INSTANCE.apmIntegrationConfig.failedArtifactTracker
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
        skywalking.determineSourceServices({
            if (it.succeeded()) {
                processServices(it.result(), handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void processServices(Set<SourceService> sourceServices, Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def sourceService : sourceServices) {
            def promise = Promise.promise()
            futures.add(promise)

            refreshServiceFailingArtifacts(sourceService, {
                if (it.succeeded()) {
                    processServiceInstances(sourceService, it.result(), promise)
                } else {
                    promise.fail(it.cause())
                }
            })
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void refreshServiceFailingArtifacts(SourceService sourceService,
                                                Handler<AsyncResult<Set<String>>> handler) {
        def start = Instant.now(), end = Instant.now(), step = "MINUTE"
        skywalking.getServiceInstances(start, end, step, sourceService.id, {
            if (it.succeeded()) {
                def serviceInstances = it.result()
                Set<String> serviceInstanceIds = new HashSet<>()
                Set<String> serviceInstanceNames = new HashSet<>()
                for (int i = 0; i < serviceInstances.size(); i++) {
                    def serviceInstance = serviceInstances.getJsonObject(i)
                    serviceInstanceIds.add(serviceInstance.getString("key"))
                    serviceInstanceNames.add(serviceInstance.getString("label"))
                }

                artifactAPI.updateFailingArtifacts(sourceService.appUuid, serviceInstanceNames, {
                    if (it.succeeded()) {
                        handler.handle(Future.succeededFuture(serviceInstanceIds))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void processServiceInstances(SourceService sourceService, Set<String> sourceServiceInstanceIds,
                                         Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def sourceServiceInstanceId : sourceServiceInstanceIds) {
            def promise = Promise.promise()
            futures.add(promise)

            processServiceInstanceFailingTraces(sourceService, sourceServiceInstanceId, promise)
        }
        CompositeFuture.all(futures).onComplete(handler)
    }

    private void processServiceInstanceFailingTraces(SourceService sourceService, String serviceInstanceId,
                                                     Handler<AsyncResult<Void>> handler) {
        def searchStartTime
        if (failedArtifactTracker.serviceLatestSearchedFailingTraces.containsKey(sourceService)) {
            searchStartTime = failedArtifactTracker.serviceLatestSearchedFailingTraces.get(sourceService)
        } else {
            searchStartTime = Instant.now()
        }
        def searchEndTime = Instant.now()

        def traceQuery = TraceQuery.builder()
                .systemRequest(true)
                .serviceId(sourceService.id)
                .serviceInstanceId(serviceInstanceId)
                .traceState("ERROR")
                .durationStart(searchStartTime)
                .durationStop(searchEndTime)
                .durationStep("SECOND").build()
        skywalking.getTraces(traceQuery, {
            if (it.succeeded()) {
                failedArtifactTracker.addServiceLatestSearchedFailingTraces(sourceService, searchEndTime)

                processFailingTraces(sourceService, it.result().traces(), handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void processFailingTraces(SourceService sourceService, List<Trace> traces,
                                      Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def trace : traces) {
            trace.traceIds().each {
                def future = Promise.promise()
                skywalking.getTraceStack(sourceService.appUuid, it, {
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

    private void processFailingTraceStack(List<TraceSpan> failingTraceSpans, Handler<AsyncResult<Void>> handler) {
        def futures = []
        for (def failingSpan : failingTraceSpans) {
            if (!failingSpan.error) {
                continue
            }
            def future = Promise.promise()
            futures.add(future)

            def appUuid = failingSpan.serviceCode()
            def endpointName = failingSpan.endpointName()
            artifactAPI.findSourceArtifact(appUuid, endpointName, {
                if (it.succeeded()) {
                    if (it.result().isPresent()) {
                        SourceArtifact artifact = it.result().get()
                        SourceArtifactStatus status
                        if (artifact.status().latestFailedServiceInstance() != null) {
                            if (artifact.status().latestFailedServiceInstance() != failingSpan.serviceInstanceName()) {
                                status = artifact.status()
                                        .withActivelyFailing(true)
                                        .withLatestFailedServiceInstance(failingSpan.serviceInstanceName())
                            }
                        } else {
                            status = artifact.status()
                                    .withActivelyFailing(true)
                                    .withLatestFailedServiceInstance(failingSpan.serviceInstanceName())
                        }

                        storage.addArtifactFailure(artifact, failingSpan, {
                            if (it.succeeded()) {
                                if (status != null) {
                                    artifactAPI.createOrUpdateSourceArtifactStatus(artifact.withStatus(status), future)
                                } else {
                                    future.complete()
                                }
                            } else {
                                future.fail(it.cause())
                            }
                        })
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
