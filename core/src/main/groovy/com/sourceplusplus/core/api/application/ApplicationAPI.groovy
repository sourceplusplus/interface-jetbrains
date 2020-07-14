package com.sourceplusplus.core.api.application

import com.fasterxml.jackson.core.type.TypeReference
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.application.SourceApplicationSubscription
import com.sourceplusplus.api.model.artifact.ArtifactSubscribeRequest
import com.sourceplusplus.api.model.artifact.SourceArtifact
import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.error.SourceAPIErrors
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.JacksonCodec
import io.vertx.ext.web.RoutingContext
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import org.apache.commons.io.IOUtils

import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Used to add/modify applications and get/refresh application subscriptions.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class ApplicationAPI extends AbstractVerticle {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}')
    private final static List<String> COLOR_NAMES =
            IOUtils.readLines(ApplicationAPI.getResourceAsStream("/appname_gen/color-list.txt"), "UTF-8")
    private final static List<String> ANIMAL_NAMES =
            IOUtils.readLines(ApplicationAPI.getResourceAsStream("/appname_gen/animal-list.txt"), "UTF-8")
    private static final Map<String, SourceApplication> SOURCE_APPLICATION_CACHE = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .expiration(5, TimeUnit.MINUTES).build()
    private final SourceCore core

    ApplicationAPI(SourceCore core) {
        this.core = Objects.requireNonNull(core)
    }

    @Override
    void start() throws Exception {
        core.baseRouter.post("/applications").handler(this.&createApplicationRoute)
        core.baseRouter.get("/applications").handler(this.&getApplicationsRoute)
        core.baseRouter.put("/applications/:appUuid").handler(this.&updateApplicationRoute)
        core.baseRouter.get("/applications/search").handler(this.&searchApplicationsRoute)
        core.baseRouter.get("/applications/:appUuid").handler(this.&getApplicationRoute)
        core.baseRouter.get("/applications/:appUuid/subscriptions").handler(this.&getApplicationSubscriptionsRoute)
        core.baseRouter.get("/applications/:appUuid/endpoints").handler(this.&getApplicationEndpointsRoute)
        core.baseRouter.get("/applications/:appUuid/subscribers/:subscriberUuid/subscriptions")
                .handler(this.&getSubscriberApplicationSubscriptionsRoute)
        core.baseRouter.put("/applications/:appUuid/subscribers/:subscriberUuid/subscriptions/refresh")
                .handler(this.&refreshSubscriberApplicationSubscriptionsRoute)
        log.info("{} started", getClass().getSimpleName())
    }

    private getSubscriberApplicationSubscriptionsRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def subscriberUuid = routingContext.request().getParam("subscriberUuid")
        if (!appUuid || !subscriberUuid) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        getSubscriberApplicationSubscriptions(appUuid, subscriberUuid, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void getSubscriberApplicationSubscriptions(String appUuid, String subscriberUuid,
                                               Handler<AsyncResult<List<ArtifactSubscribeRequest>>> handler) {
        def message = new JsonObject()
        message.put("app_uuid", appUuid)
        message.put('subscriber_uuid', subscriberUuid)
        vertx.eventBus().request(ArtifactSubscriptionTracker.GET_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, message, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(JacksonCodec.decodeValue(it.result().body().toString(),
                        new TypeReference<List<ArtifactSubscribeRequest>>() {
                        })))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private refreshSubscriberApplicationSubscriptionsRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        def subscriberUuid = routingContext.request().getParam("subscriberUuid")
        if (!appUuid || !subscriberUuid) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        refreshSubscriberApplicationSubscriptions(appUuid, subscriberUuid, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200).end()
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void refreshSubscriberApplicationSubscriptions(String appUuid, String subscriberUuid,
                                                   Handler<AsyncResult<Void>> handler) {
        def message = new JsonObject()
        message.put("app_uuid", appUuid)
        message.put('subscriber_uuid', subscriberUuid)
        vertx.eventBus().request(ArtifactSubscriptionTracker.REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, message, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture())
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private getApplicationSubscriptionsRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        if (!appUuid) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }
        def includeAutomatic = Boolean.valueOf(routingContext.request().getParam("includeAutomatic"))

        getApplicationSubscriptions(appUuid, includeAutomatic, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    private getApplicationEndpointsRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        if (!appUuid) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }
        def includeAutomatic = Boolean.valueOf(routingContext.request().getParam("includeAutomatic"))

        getApplicationEndpoints(appUuid, includeAutomatic, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void getApplicationSubscriptions(String appUuid, boolean includeAutomatic,
                                     Handler<AsyncResult<Set<SourceApplicationSubscription>>> handler) {
        log.info("Getting appliction subscriptions. App UUID: $appUuid - Include automatic: $includeAutomatic")
        core.storage.getApplicationSubscriptions(appUuid, {
            if (it.succeeded()) {
                def subscribers = it.result()
                core.storage.findArtifactBySubscribeAutomatically(appUuid, {
                    if (it.succeeded()) {
                        def automaticSubscriptions = it.result()
                        def mergeMap = new HashMap<String, SourceApplicationSubscription.Builder>()
                        subscribers.each {
                            mergeMap.putIfAbsent(it.artifactQualifiedName(),
                                    SourceApplicationSubscription.builder().from(it))
                        }
                        automaticSubscriptions.each {
                            if (mergeMap.containsKey(it.artifactQualifiedName())) {
                                mergeMap.get(it.artifactQualifiedName())
                                        .automaticSubscription(Boolean.valueOf(it.config().subscribeAutomatically()))
                            } else {
                                mergeMap.putIfAbsent(it.artifactQualifiedName(),
                                        SourceApplicationSubscription.builder()
                                                .artifactQualifiedName(it.artifactQualifiedName())
                                                .subscribers(0)
                                                .automaticSubscription(Boolean.valueOf(it.config().subscribeAutomatically())))
                            }
                        }

                        def mergedSubscriptions = mergeMap.collect { it.value.build() } as Set
                        if (!includeAutomatic) {
                            mergedSubscriptions.removeIf {
                                Boolean.valueOf(it.automaticSubscription()) && it.subscribers() == 0
                            }
                        }
                        handler.handle(Future.succeededFuture(mergedSubscriptions))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    void getApplicationEndpoints(String appUuid, boolean includeAutomatic,
                                 Handler<AsyncResult<List<SourceArtifact>>> handler) {
        log.info("Getting appliction endpoints. App UUID: $appUuid - Include automatic: $includeAutomatic")
        core.storage.findArtifactByEndpoint(appUuid, includeAutomatic, handler)
    }

    private void updateApplicationRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        if (!appUuid) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        SourceApplication request
        try {
            request = Json.decodeValue(routingContext.getBodyAsJson()
                    .put("create_request", false)
                    .put("isCreateRequest", false)
                    .put("update_request", true)
                    .put("isUpdateRequest", true).toString(), SourceApplication.class)
        } catch (all) {
            all.printStackTrace()
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }
        request = request.withAppUuid(appUuid)

        updateApplication(request, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void updateApplication(SourceApplication updateRequest, Handler<AsyncResult<SourceApplication>> handler) {
        log.info(String.format("Updating application. App uuid: %s - App name: %s",
                updateRequest.appUuid(), updateRequest.appName()))
        core.storage.updateApplication(updateRequest, {
            if (it.succeeded()) {
                def application = it.result()
                SOURCE_APPLICATION_CACHE.put(application.appUuid(), application)
                handler.handle(Future.succeededFuture(application))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    private void createApplicationRoute(RoutingContext routingContext) {
        SourceApplication request
        try {
            request = Json.decodeValue(routingContext.getBodyAsJson()
                    .put("create_request", true)
                    .put("isCreateRequest", true)
                    .put("update_request", false)
                    .put("isUpdateRequest", false).toString(), SourceApplication.class)
        } catch (all) {
            all.printStackTrace()
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
            return
        }

        createApplication(request, {
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void createApplication(SourceApplication createRequest, Handler<AsyncResult<SourceApplication>> handler) {
        //todo: restrict creating applications with same name
        //generate application name (if necessary)
        if (createRequest.appName() == null || createRequest.appName().isEmpty()) {
            createRequest = createRequest.withAppName(generateApplicationName())
        }
        createRequest = createRequest.withCreateDate(Instant.now())

        if (createRequest.appUuid() != null) {
            //creating with specific appUuid, verify
            if (UUID_PATTERN.matcher(createRequest.appUuid()).matches()) {
                getApplication(createRequest.appUuid(), {
                    if (it.succeeded()) {
                        if (it.result().isPresent()) {
                            handler.handle(Future.failedFuture(new IllegalArgumentException("Application already exists")))
                        } else {
                            createRequest = createRequest.withIsCreateRequest(null)
                                    .withIsUpdateRequest(null)
                            log.info(String.format("Creating application. App uuid: %s - App name: %s",
                                    createRequest.appUuid(), createRequest.appName()))
                            core.storage.createApplication(createRequest, {
                                if (it.succeeded()) {
                                    def application = it.result()
                                    SOURCE_APPLICATION_CACHE.put(application.appUuid(), application)
                                    handler.handle(Future.succeededFuture(application))
                                } else {
                                    handler.handle(Future.failedFuture(it.cause()))
                                }
                            })
                        }
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(new IllegalArgumentException("Invalid application UUID")))
            }
        } else {
            createRequest = createRequest.withAppUuid(UUID.randomUUID().toString())
                    .withIsCreateRequest(null)
                    .withIsUpdateRequest(null)
            log.info(String.format("Creating application. App uuid: %s - App name: %s",
                    createRequest.appUuid(), createRequest.appName()))
            core.storage.createApplication(createRequest, {
                if (it.succeeded()) {
                    def application = it.result()
                    SOURCE_APPLICATION_CACHE.put(application.appUuid(), application)
                    handler.handle(Future.succeededFuture(application))
                } else {
                    handler.handle(Future.failedFuture(it.cause()))
                }
            })
        }
    }

    private void getApplicationRoute(RoutingContext routingContext) {
        def appUuid = routingContext.request().getParam("appUuid")
        if (!appUuid) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
        } else {
            getApplication(appUuid, {
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
    }

    private void searchApplicationsRoute(RoutingContext routingContext) {
        def appName = routingContext.request().getParam("appName")
        if (!appName) {
            routingContext.response().setStatusCode(400)
                    .end(Json.encode(new SourceAPIError().addError(SourceAPIErrors.INVALID_INPUT)))
        } else {
            searchApplications(appName, {
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
    }

    void searchApplications(String appName, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        log.info("Searching for application. App name: {}", appName)
        core.storage.findApplicationByName(appName, handler)
    }

    void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        log.trace("Getting application. App uuid: {}", appUuid)
        getAndCacheSourceApplication(appUuid, handler)
    }

    private void getApplicationsRoute(RoutingContext routingContext) {
        getApplications({
            if (it.succeeded()) {
                routingContext.response().setStatusCode(200)
                        .end(Json.encode(it.result()))
            } else {
                routingContext.response().setStatusCode(400)
                        .end(Json.encode(new SourceAPIError().addError(it.cause().message)))
            }
        })
    }

    void getApplications(Handler<AsyncResult<List<SourceApplication>>> handler) {
        log.info("Getting all applications")
        core.storage.getAllApplications(handler)
    }

    private void getAndCacheSourceApplication(String appUuid,
                                              Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        if (SOURCE_APPLICATION_CACHE.containsKey(appUuid)) {
            handler.handle(Future.succeededFuture(Optional.ofNullable(SOURCE_APPLICATION_CACHE.get(appUuid))))
        } else {
            log.info("Getting source application from storage. App UUID: {}", appUuid)
            core.storage.getApplication(appUuid, {
                if (it.failed()) {
                    handler.handle(Future.failedFuture(it.cause()))
                } else {
                    if (it.result().isPresent()) {
                        SOURCE_APPLICATION_CACHE.put(appUuid, it.result().get())
                    } else {
                        SOURCE_APPLICATION_CACHE.put(appUuid, null)
                    }
                    handler.handle(Future.succeededFuture(it.result()))
                }
            })
        }
    }

    /**
     * Generates a random application name (ex. red-goat).
     *
     * @return the generated application name
     */
    static String generateApplicationName() {
        def random = ThreadLocalRandom.current()
        def color = COLOR_NAMES.get(random.nextInt(COLOR_NAMES.size()))
        def animal = ANIMAL_NAMES.get(random.nextInt(ANIMAL_NAMES.size()))
        return color + "-" + animal
    }
}
