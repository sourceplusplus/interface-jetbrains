package com.sourceplusplus.core.api.artifact

import com.fasterxml.jackson.core.type.TypeReference
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscription
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest
import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.error.SourceAPIErrors
import com.sourceplusplus.api.model.internal.ApplicationArtifact
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.JacksonCodec
import io.vertx.ext.web.RoutingContext
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap

import java.time.Instant
import java.util.concurrent.TimeUnit

import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED
import static com.sourceplusplus.api.bridge.PluginBridgeEndpoints.ARTIFACT_STATUS_UPDATED

/**
 * Used to add/modify/fetch artifact subscriptions and configurations.
 * todo: artifact caching
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class ArtifactAPI extends AbstractVerticle {

    private static final Map<ApplicationArtifact, SourceArtifact> APPLICATION_ARTIFACT_CACHE = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .expiration(5, TimeUnit.MINUTES).build()
    private static final Map<String, SourceArtifact> ENDPOINT_NAME_ARTIFACT_CACHE = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .expiration(5, TimeUnit.MINUTES).build()
    private static final Map<String, SourceArtifact> ENDPOINT_ID_ARTIFACT_CACHE = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .expiration(5, TimeUnit.MINUTES).build()
    private final SourceCore core

    ArtifactAPI(SourceCore core) {
        this.core = Objects.requireNonNull(core)
    }

    @Override
    void start(Promise<Void> startFuture) throws Exception {
        core.baseRouter.get("/applications/:appUuid/artifacts")
                .handler(this.&getApplicationSourceArtifactsRoute)
        core.baseRouter.post("/applications/:appUuid/artifacts")
                .handler(this.&createOrUpdateSourceArtifactRoute)
        core.baseRouter.get("/applications/:appUuid/artifacts/:artifactQualifiedName")
                .handler(this.&getSourceArtifactRoute)
        core.baseRouter.put("/applications/:appUuid/artifacts/:artifactQualifiedName/config")
                .handler(this.&createOrUpdateSourceArtifactConfigRoute)
        core.baseRouter.get("/applications/:appUuid/artifacts/:artifactQualifiedName/config")
                .handler(this.&getSourceArtifactConfigRoute)
        core.baseRouter.put("/applications/:appUuid/artifacts/:artifactQualifiedName/unsubscribe")
                .handler(this.&unsubscribeSourceArtifactRoute)
        core.baseRouter.get("/applications/:appUuid/artifacts/:artifactQualifiedName/subscriptions")
                .handler(this.&getSourceArtifactSubscriptionsRoute)

        def subscriptionTracker = new ArtifactSubscriptionTracker(core)
        vertx.deployVerticle(subscriptionTracker, new DeploymentOptions().setConfig(config()), startFuture)
        log.info("{} started", getClass().getSimpleName())
    }

    private getSourceArtifactSubscriptionsRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"), "UTF-8")
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        def appArtifact = ApplicationArtifact.builder().appUuid(appUuid)
                .artifactQualifiedName(artifactQualifiedName).build()
        getSourceArtifactSubscriptions(appArtifact, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void getSourceArtifactSubscriptions(ApplicationArtifact applicationArtifact,
                                        Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {
        vertx.eventBus().request(ArtifactSubscriptionTracker.GET_ARTIFACT_SUBSCRIPTIONS, applicationArtifact, {
            if (it.succeeded()) {
                def subscribers = JacksonCodec.decodeValue(it.result().body() as String,
                        new TypeReference<List<SourceArtifactSubscription>>() {})
                handler.handle(Future.succeededFuture(subscribers))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void unsubscribeSourceArtifactRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"), "UTF-8")
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        SourceArtifactUnsubscribeRequest request
        try {
            request = Json.decodeValue(routingContext.getBodyAsString(), SourceArtifactUnsubscribeRequest.class)
        } catch (all) {
            all.printStackTrace()
            routingContext.response().setStatusCode(500)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }
        request = request.withAppUuid(appUuid).withArtifactQualifiedName(artifactQualifiedName)

        unsubscribeSourceArtifact(request, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    private void unsubscribeSourceArtifact(SourceArtifactUnsubscribeRequest request,
                                           Handler<AsyncResult<Boolean>> handler) {
        vertx.eventBus().request(ArtifactSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT, request, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(true))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void createOrUpdateSourceArtifactRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        if (!appUuid) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        SourceArtifact request
        try {
            request = Json.decodeValue(routingContext.getBodyAsString(), SourceArtifact.class)
        } catch (all) {
            all.printStackTrace()
            routingContext.response().setStatusCode(500)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }
        request = request.withAppUuid(appUuid)

        createOrUpdateSourceArtifact(request, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void createOrUpdateSourceArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {
        getAndCacheSourceArtifact(artifact.appUuid(), artifact.artifactQualifiedName(), {
            if (it.succeeded()) {
                def now = Instant.now()
                if (it.result().isPresent()) {
                    //update
                    def oldArtifact = it.result().get()
                    def oldConfig = oldArtifact.config()
                    def newConfig = artifact.config()
                    def oldStatus = oldArtifact.status()
                    def newStatus = artifact.status()

                    artifact = artifact.withLastUpdated(now)
                    if (artifact.config().endpointIds() != null) {
                        if (artifact.config().endpointIds().isEmpty()) {
                            newConfig = newConfig.withEndpointIds()
                        } else {
                            if (oldConfig.endpointIds() == null) {
                                newConfig = newConfig.withEndpointIds(artifact.config().endpointIds())
                            } else {
                                newConfig = newConfig.withEndpointIds(
                                        oldConfig.endpointIds() + artifact.config().endpointIds())
                            }
                        }
                    }
                    artifact = artifact.withConfig(newConfig)

                    core.storage.updateArtifact(artifact, {
                        if (it.succeeded()) {
                            def appArtifact = ApplicationArtifact.builder().appUuid(artifact.appUuid())
                                    .artifactQualifiedName(artifact.artifactQualifiedName()).build()
                            APPLICATION_ARTIFACT_CACHE.put(appArtifact, it.result())

                            if (it.result().config() && it.result().config().endpointName()) {
                                ENDPOINT_NAME_ARTIFACT_CACHE.put(artifact.appUuid() + ":" + it.result().config().endpointName(), it.result())
                            }
                            if (it.result().config() && it.result().config().endpointIds()) {
                                it.result().config().endpointIds().each { endpointId ->
                                    ENDPOINT_ID_ARTIFACT_CACHE.put(artifact.appUuid() + ":" + endpointId, it.result())
                                }
                            }

                            def artifactConfigUpdated = false
                            if (oldConfig == null && newConfig == null) {
                                //no update
                            } else if (oldConfig != null && newConfig == null) {
                                //no update
                            } else if (oldConfig == null && newConfig != null) {
                                artifactConfigUpdated = true
                            } else if (Json.encode(oldConfig) != Json.encode(newConfig)) {
                                artifactConfigUpdated = true
                            } else {
                                //no update
                            }
                            if (artifactConfigUpdated) {
                                vertx.eventBus().publish(ARTIFACT_CONFIG_UPDATED.address,
                                        new JsonObject(Json.encode(it.result())))
                            }

                            def artifactStatusUpdated = false
                            if (oldStatus == null && newStatus == null) {
                                //no update
                            } else if (oldStatus != null && newStatus == null) {
                                artifactStatusUpdated = true
                            } else if (oldStatus == null && newStatus != null) {
                                artifactStatusUpdated = true
                            } else if (Json.encode(oldStatus) != Json.encode(newStatus)) {
                                artifactStatusUpdated = true
                            } else {
                                //no update
                            }
                            if (artifactStatusUpdated) {
                                vertx.eventBus().publish(ARTIFACT_STATUS_UPDATED.address,
                                        new JsonObject(Json.encode(it.result())))
                            }

                            handler.handle(Future.succeededFuture(it.result()))
                        } else {
                            handler.handle(Future.failedFuture(it.cause()))
                        }
                    })
                } else {
                    //create
                    artifact = artifact.withCreateDate(now).withLastUpdated(now)
                    core.storage.createArtifact(artifact, {
                        if (it.succeeded()) {
                            def appArtifact = ApplicationArtifact.builder().appUuid(artifact.appUuid())
                                    .artifactQualifiedName(artifact.artifactQualifiedName()).build()
                            APPLICATION_ARTIFACT_CACHE.put(appArtifact, it.result())

                            if (it.result().config() && it.result().config().endpointName()) {
                                ENDPOINT_NAME_ARTIFACT_CACHE.put(artifact.appUuid() + ":" + it.result().config().endpointName(), it.result())
                            }
                            if (it.result().config() && it.result().config().endpointIds()) {
                                it.result().config().endpointIds().each { endpointId ->
                                    ENDPOINT_ID_ARTIFACT_CACHE.put(artifact.appUuid() + ":" + endpointId, it.result())
                                }
                            }
                            handler.handle(Future.succeededFuture(it.result()))
                        } else {
                            handler.handle(Future.failedFuture(it.cause()))
                        }
                    })
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void getApplicationSourceArtifactsRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        if (!appUuid) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        def includeOnlyFailing = Boolean.valueOf(routingContext.request().getParam("includeOnlyFailing"))
        if (includeOnlyFailing) {
            getFailingArtifacts(appUuid, {
                if (it.succeeded()) {
                    routingContext.response().setStatusCode(200)
                            .end(Json.encode(it.result()))
                } else {
                    routingContext.response().setStatusCode(400)
                            .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
                }
            })
        } else {
            getApplicationSourceArtifacts(appUuid, {
                if (it.succeeded()) {
                    routingContext.response().setStatusCode(200)
                            .end(Json.encode(it.result()))
                } else {
                    routingContext.response().setStatusCode(400)
                            .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
                }
            })
        }
    }

    void getApplicationSourceArtifacts(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        log.info("Getting all of application's source artifacts. App UUID: {}", appUuid)
        core.storage.getApplicationArtifacts(appUuid, handler)
    }

    private void getSourceArtifactRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"), "UTF-8")
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        getSourceArtifact(appUuid, artifactQualifiedName, {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    routingContext.response().setStatusCode(200)
                            .end(Json.encode(it.result()))
                } else {
                    routingContext.response().setStatusCode(404).end()
                }
            } else {
                routingContext.response().setStatusCode(400)
                        .end(it.cause().message)
            }
        })
    }

    void findSourceArtifact(String appUuid, String search,
                            Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        getSourceArtifact(appUuid, search, {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    handler.handle(Future.succeededFuture(it.result()))
                } else {
                    getSourceArtifactByEndpointName(appUuid, search, {
                        if (it.succeeded()) {
                            handler.handle(Future.succeededFuture(it.result()))
                        } else {
                            handler.handle(Future.failedFuture(it.cause()))
                        }
                    })
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    void getSourceArtifact(String appUuid, String artifactQualifiedName,
                           Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        log.debug("Getting source artifact. App UUID: {} - Artifact qualified name: {}", appUuid, artifactQualifiedName)
        getAndCacheSourceArtifact(appUuid, artifactQualifiedName, handler)
    }

    void getSourceArtifactByEndpointName(String appUuid, String endpointName,
                                         Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        log.debug("Getting source artifact. App UUID: {} - Endpoint name: {}", appUuid, endpointName)
        getAndCacheSourceArtifactByEndpointName(appUuid, endpointName, handler)
    }

    void getSourceArtifactByEndpointId(String appUuid, String endpointId,
                                       Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        log.debug("Getting source artifact. App UUID: {} - Endpoint id: {}", appUuid, endpointId)
        getAndCacheSourceArtifactByEndpointId(appUuid, endpointId, handler)
    }

    void getFailingArtifacts(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        log.info("Getting failing source artifacts. App UUID: {}", appUuid)
        core.storage.findArtifactByFailing(appUuid, handler)
    }

    void updateFailingArtifacts(String appUuid, Set<String> activeServiceInstances,
                                Handler<AsyncResult<Void>> handler) {
        if (activeServiceInstances.isEmpty()) {
            handler.handle(Future.succeededFuture())
        } else {
            core.storage.findArtifactByFailing(appUuid, {
                if (it.succeeded()) {
                    def failingArtifacts = it.result()
                    if (failingArtifacts.isEmpty()) {
                        handler.handle(Future.succeededFuture())
                    } else {
                        def futures = []
                        failingArtifacts.each {
                            if (!activeServiceInstances.contains(it.status().latestFailedSpan().serviceInstanceName())) {
                                def future = Promise.promise()
                                futures.add(future)

                                def updatedArtifact = it.withStatus(it.status().withLatestFailedSpan(null))
                                createOrUpdateSourceArtifact(updatedArtifact, future)
                            }
                        }
                        CompositeFuture.all(futures).onComplete({
                            if (it.succeeded()) {
                                handler.handle(Future.succeededFuture())
                            } else {
                                handler.handle(Future.failedFuture(it.cause()))
                            }
                        })
                    }
                } else {
                    handler.handle(Future.failedFuture(it.cause()))
                }
            })
        }
    }

    private createOrUpdateSourceArtifactConfigRoute(RoutingContext routingContext) {
        def appUuid = routingContext.pathParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"), "UTF-8")
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        SourceArtifactConfig updatedArtifactConfig
        try {
            updatedArtifactConfig = Json.decodeValue(routingContext.getBodyAsString(), SourceArtifactConfig.class)
        } catch (all) {
            all.printStackTrace()
            routingContext.response().setStatusCode(500)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        createOrUpdateSourceArtifactConfig(appUuid, artifactQualifiedName, updatedArtifactConfig, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void createOrUpdateSourceArtifactConfig(String appUuid, String artifactQualifiedName,
                                            SourceArtifactConfig artifactConfig,
                                            Handler<AsyncResult<SourceArtifactConfig>> handler) {
        def artifactWithConfig = SourceArtifact.builder()
                .appUuid(appUuid)
                .artifactQualifiedName(artifactQualifiedName)
                .config(artifactConfig).build()
        createOrUpdateSourceArtifact(artifactWithConfig, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(it.result().config()))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private getSourceArtifactConfigRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"), "UTF-8")
        if (!appUuid || !artifactQualifiedName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        getSourceArtifactConfig(appUuid, artifactQualifiedName, {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    routingContext.response().setStatusCode(200)
                            .end(Json.encode(it.result()))
                } else {
                    routingContext.response().setStatusCode(404).end()
                }
            } else {
                routingContext.response().setStatusCode(400)
                        .end(it.cause().message)
            }
        })
    }

    void getSourceArtifactConfig(String appUuid, String artifactQualifiedName,
                                 Handler<AsyncResult<Optional<SourceArtifactConfig>>> handler) {
        log.debug("Getting source artifact config. App UUID: {} - Artifact qualified name: {}",
                appUuid, artifactQualifiedName)
        getAndCacheSourceArtifact(appUuid, artifactQualifiedName, {
            if (it.succeeded() && it.result().isPresent()) {
                handler.handle(Future.succeededFuture(Optional.ofNullable(it.result().get().config())))
            } else {
                handler.handle(it)
            }
        })
    }

    private void getAndCacheSourceArtifact(String appUuid, String artifactQualifiedName,
                                           Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        def appArtifact = ApplicationArtifact.builder().appUuid(appUuid)
                .artifactQualifiedName(artifactQualifiedName).build()
        if (APPLICATION_ARTIFACT_CACHE.containsKey(appArtifact)) {
            handler.handle(Future.succeededFuture(Optional.ofNullable(
                    APPLICATION_ARTIFACT_CACHE.get(appArtifact))))
        } else {
            log.info("Getting source artifact from storage. App UUID: {} - Artifact qualified name: {}",
                    appUuid, artifactQualifiedName)
            core.storage.getArtifact(appUuid, artifactQualifiedName, {
                if (it.failed()) {
                    handler.handle(Future.failedFuture(it.cause()))
                } else {
                    if (it.result().isPresent()) {
                        APPLICATION_ARTIFACT_CACHE.put(appArtifact, it.result().get())
                    } else {
                        APPLICATION_ARTIFACT_CACHE.put(appArtifact, null)
                    }
                    handler.handle(Future.succeededFuture(it.result()))
                }
            })
        }
    }

    private void getAndCacheSourceArtifactByEndpointName(String appUuid, String endpointName,
                                                         Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        if (ENDPOINT_NAME_ARTIFACT_CACHE.containsKey(appUuid + ":" + endpointName)) {
            handler.handle(Future.succeededFuture(Optional.ofNullable(
                    ENDPOINT_NAME_ARTIFACT_CACHE.get(appUuid + ":" + endpointName))))
        } else {
            log.info("Getting source artifact from storage. App UUID: {} - Endpoint name: {}", appUuid, endpointName)
            core.storage.findArtifactByEndpointName(appUuid, endpointName, {
                if (it.failed()) {
                    handler.handle(Future.failedFuture(it.cause()))
                } else {
                    if (it.result().isPresent()) {
                        ENDPOINT_NAME_ARTIFACT_CACHE.put(appUuid + ":" + endpointName, it.result().get())
                    } else {
                        ENDPOINT_NAME_ARTIFACT_CACHE.put(appUuid + ":" + endpointName, null)
                    }
                    handler.handle(Future.succeededFuture(it.result()))
                }
            })
        }
    }

    private void getAndCacheSourceArtifactByEndpointId(String appUuid, String endpointId,
                                                       Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        if (ENDPOINT_ID_ARTIFACT_CACHE.containsKey(appUuid + ":" + endpointId)) {
            handler.handle(Future.succeededFuture(Optional.ofNullable(
                    ENDPOINT_ID_ARTIFACT_CACHE.get(appUuid + ":" + endpointId))))
        } else {
            log.info("Getting source artifact from storage. App UUID: {} - Endpoint id: {}", appUuid, endpointId)
            core.storage.findArtifactByEndpointId(appUuid, endpointId, {
                if (it.failed()) {
                    handler.handle(Future.failedFuture(it.cause()))
                } else {
                    if (it.result().isPresent()) {
                        ENDPOINT_ID_ARTIFACT_CACHE.put(appUuid + ":" + endpointId, it.result().get())
                    } else {
                        ENDPOINT_ID_ARTIFACT_CACHE.put(appUuid + ":" + endpointId, null)
                    }
                    handler.handle(Future.succeededFuture(it.result()))
                }
            })
        }
    }
}
