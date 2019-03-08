package com.sourceplusplus.core.api.application

import com.fasterxml.jackson.core.type.TypeReference
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.application.SourceApplicationSubscription
import com.sourceplusplus.api.model.artifact.SubscriberSourceArtifactSubscription
import com.sourceplusplus.api.model.error.SourceAPIError
import com.sourceplusplus.api.model.error.SourceAPIErrors
import com.sourceplusplus.core.api.artifact.subscription.ArtifactSubscriptionTracker
import com.sourceplusplus.core.storage.ElasticsearchDAO
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import java.util.regex.Pattern

/**
 * todo: description
 *
 * @version 0.1.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ApplicationAPI extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    private static final Pattern UUID_PATTERN = Pattern.compile(
            '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}')
    private final static List<String> COLOR_NAMES =
            IOUtils.readLines(ApplicationAPI.getResourceAsStream("/appname_gen/color-list.txt"), "UTF-8")
    private final static List<String> ANIMAL_NAMES =
            IOUtils.readLines(ApplicationAPI.getResourceAsStream("/appname_gen/animal-list.txt"), "UTF-8")
    private final Router baseRouter
    private final ElasticsearchDAO elasticsearch

    ApplicationAPI(Router baseRouter, ElasticsearchDAO elasticsearch) {
        this.baseRouter = baseRouter
        this.elasticsearch = elasticsearch
    }

    @Override
    void start() throws Exception {
        baseRouter.post("/applications").handler(this.&createApplicationRoute)
        baseRouter.get("/applications").handler(this.&getApplicationsRoute)
        baseRouter.put("/applications/:appUuid").handler(this.&updateApplicationRoute)
        baseRouter.get("/applications/:appUuid").handler(this.&getApplicationRoute)
        baseRouter.get("/applications/:appUuid/subscriptions").handler(this.&getApplicationSubscriptionsRoute)
        baseRouter.get("/applications/:appUuid/subscribers/:subscriberUuid/subscriptions")
                .handler(this.&getSubscriberApplicationSubscriptionsRoute)
        baseRouter.put("/applications/:appUuid/subscribers/:subscriberUuid/subscriptions/refresh")
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
                                               Handler<AsyncResult<List<SubscriberSourceArtifactSubscription>>> handler) {
        def message = new JsonObject()
        message.put("app_uuid", appUuid)
        message.put('subscriber_uuid', subscriberUuid)
        vertx.eventBus().send(ArtifactSubscriptionTracker.GET_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, message, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(Json.decodeValue(it.result().body().toString(),
                        new TypeReference<List<SubscriberSourceArtifactSubscription>>() {
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
        vertx.eventBus().send(ArtifactSubscriptionTracker.REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, message, {
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
        def includeAutomatic = routingContext.request().getParam("includeAutomatic") as boolean

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

    void getApplicationSubscriptions(String appUuid, boolean includeAutomatic,
                                     Handler<AsyncResult<Set<SourceApplicationSubscription>>> handler) {
        vertx.eventBus().send(ArtifactSubscriptionTracker.GET_APPLICATION_SUBSCRIPTIONS, appUuid, {
            if (it.succeeded()) {
                def subscribers = Json.decodeValue(it.result().body() as String,
                        new TypeReference<Set<SourceApplicationSubscription>>() {})
                if (includeAutomatic) {
                    elasticsearch.findArtifactBySubscribeAutomatically(appUuid, {
                        if (it.succeeded()) {
                            def result = it.result()
                            result.each {
                                subscribers.add(SourceApplicationSubscription.builder()
                                        .artifactQualifiedName(it.artifactQualifiedName())
                                        .subscribers(-1)
                                        .automaticSubscription(true)
                                        .build())
                            }
                            handler.handle(Future.succeededFuture(subscribers))
                        } else {
                            handler.handle(Future.failedFuture(it.cause()))
                        }
                    })
                } else {
                    handler.handle(Future.succeededFuture(subscribers))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
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
        elasticsearch.updateApplication(updateRequest, handler)
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
                            elasticsearch.createApplication(createRequest, handler)
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
            elasticsearch.createApplication(createRequest, handler)
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

    /**
     * Retrieve {@link SourceApplication} by an existing {@link SourceApplication#appUuid}.
     *
     * @param appUuid the {@link SourceApplication#appUuid} to retrieve
     * @param handler executed with {@link SourceApplication} if found, empty Optional otherwise
     */
    void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        log.info("Getting application. App uuid: {}", appUuid)
        elasticsearch.getApplication(appUuid, handler)
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
        elasticsearch.getAllApplications(handler)
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
