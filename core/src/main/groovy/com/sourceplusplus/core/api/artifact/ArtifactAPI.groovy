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

    private static final Map<ApplicationArtifact, SourceArtifact> ARTIFACT_CACHE = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .expiration(1, TimeUnit.MINUTES).build()
    private final SourceCore core

    ArtifactAPI(SourceCore core) {
        this.core = Objects.requireNonNull(core)
    }

    @Override
    void start(Promise<Void> startFuture) throws Exception {
        core.baseRouter.get("/applications/:appUuid/artifacts")
                .handler(this.&getApplicationSourceArtifactsRoute)
        core.baseRouter.post("/applications/:appUuid/artifacts/:artifactQualifiedName")
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
        def artifactQualifiedName = URLDecoder.decode(routingContext.pathParam("artifactQualifiedName"), "UTF-8")
        if (!appUuid || !artifactQualifiedName) {
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
        request = request.withAppUuid(appUuid).withArtifactQualifiedName(artifactQualifiedName)

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
                    artifact = artifact.withLastUpdated(now)
                    if (artifact.config() != null && oldArtifact.config() != null) {
                        def oldConfig = oldArtifact.config()
                        def newConfig = artifact.config()
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
                    }
                    core.storage.updateArtifact(artifact, {
                        if (it.succeeded()) {
                            def appArtifact = ApplicationArtifact.builder().appUuid(artifact.appUuid())
                                    .artifactQualifiedName(artifact.artifactQualifiedName()).build()
                            ARTIFACT_CACHE.put(appArtifact, it.result())
                        }
                        handler.handle(it)
                    })
                } else {
                    //create
                    artifact = artifact.withCreateDate(now).withLastUpdated(now)
                    core.storage.createArtifact(artifact, {
                        if (it.succeeded()) {
                            def appArtifact = ApplicationArtifact.builder().appUuid(artifact.appUuid())
                                    .artifactQualifiedName(artifact.artifactQualifiedName()).build()
                            ARTIFACT_CACHE.put(appArtifact, it.result())
                        }
                        handler.handle(it)
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

    void getSourceArtifact(String appUuid, String artifactQualifiedName,
                           Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        log.debug("Getting source artifact. App UUID: {} - Artifact qualified name: {}", appUuid, artifactQualifiedName)
        getAndCacheSourceArtifact(appUuid, artifactQualifiedName, handler)
    }

    void getSourceArtifactByEndpointName(String appUuid, String endpointName,
                                         Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        log.info("Getting source artifact. App UUID: {} - Endpoint name: {}", appUuid, endpointName)
        core.storage.findArtifactByEndpointName(appUuid, endpointName, handler)
    }

    void getSourceArtifactByEndpointId(String appUuid, String endpointId,
                                       Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        log.info("Getting source artifact. App UUID: {} - Endpoint id: {}", appUuid, endpointId)
        core.storage.findArtifactByEndpointId(appUuid, endpointId, handler)
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
                def artifact = it.result()
                handler.handle(Future.succeededFuture(artifact.config()))

                //artifact config updated
                vertx.eventBus().publish(ARTIFACT_CONFIG_UPDATED.address, new JsonObject(Json.encode(artifact)))
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
        def cachedArtifact = ARTIFACT_CACHE.get(appArtifact)
        if (cachedArtifact) {
            handler.handle(Future.succeededFuture(Optional.of(cachedArtifact)))
        } else {
            log.info("Getting source artifact from storage. App UUID: {} - Artifact qualified name: {}",
                    appUuid, artifactQualifiedName)
            core.storage.getArtifact(appUuid, artifactQualifiedName, {
                if (it.failed()) {
                    handler.handle(Future.failedFuture(it.cause()))
                } else {
                    if (it.result().isPresent()) {
                        ARTIFACT_CACHE.put(appArtifact, it.result().get())
                    }
                    handler.handle(Future.succeededFuture(it.result()))
                }
            })
        }
    }
}
