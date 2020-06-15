package com.sourceplusplus.core.api.artifact.subscription

import com.sourceplusplus.api.model.artifact.*
import com.sourceplusplus.api.model.internal.ApplicationArtifact
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest
import com.sourceplusplus.api.model.trace.ArtifactTraceUnsubscribeRequest
import com.sourceplusplus.core.SourceCore
import groovy.util.logging.Slf4j
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

import static com.sourceplusplus.core.api.metric.track.MetricSubscriptionTracker.*
import static com.sourceplusplus.core.api.trace.track.TraceSubscriptionTracker.*

/**
 * Keeps track of artifact subscriptions.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class ArtifactSubscriptionTracker extends AbstractVerticle {

    public static final String SUBSCRIBE_TO_ARTIFACT = "SubscribeToArtifact"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT = "UnsubscribeFromArtifact"
    public static final String GET_ARTIFACT_SUBSCRIPTIONS = "GetArtifactSubscriptions"
    public static final String REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS = "RefreshSubscriberApplicationSubscriptions"
    public static final String GET_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS = "GetSubscriberApplicationSubscriptions"

    protected final SourceCore core

    ArtifactSubscriptionTracker(SourceCore core) {
        this.core = Objects.requireNonNull(core)
    }

    @Override
    void start() throws Exception {
        if (config().getJsonObject("core").getInteger("subscription_inactive_limit_minutes") > 0) {
            vertx.setPeriodic(TimeUnit.SECONDS.toMillis(10), {
                removeInactiveArtifactSubscriptions()
            })
        }
        vertx.setPeriodic(TimeUnit.SECONDS.toMillis(5), {
            core.storage.getSubscriberArtifactSubscriptions({
                if (it.succeeded()) {
                    it.result().keySet().each {
                        if (it.type == SourceArtifactSubscriptionType.METRICS) {
                            vertx.eventBus().publish(PUBLISH_ARTIFACT_METRICS, it)
                        } else {
                            vertx.eventBus().publish(PUBLISH_ARTIFACT_TRACES, it)
                        }
                    }
                } else {
                    log.error("Failed to get subscriber artifact subscriptions", it.cause())
                }
            })
        })
        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT, {
            subscribeToArtifact(it as Message<ArtifactSubscribeRequest>)
        })
        vertx.eventBus().consumer(UNSUBSCRIBE_FROM_ARTIFACT, {
            unsubscribeFromArtifact(it as Message<ArtifactUnsubscribeRequest>)
        })
        vertx.eventBus().consumer(GET_ARTIFACT_SUBSCRIPTIONS, { msg ->
            def appArtifact = msg.body() as ApplicationArtifact
            core.storage.getArtifactSubscriptions(appArtifact.appUuid(), appArtifact.artifactQualifiedName(), {
                if (it.succeeded()) {
                    msg.reply(Json.encode(it.result()))
                } else {
                    log.error("Failed to get artifact subscriptions", it.cause())
                    msg.fail(500, it.cause().message)
                }
            })
        })
        vertx.eventBus().consumer(REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, { msg ->
            def request = msg.body() as JsonObject
            def appUuid = request.getString("app_uuid")
            def subscriberUuid = request.getString("subscriber_uuid")

            core.storage.getSubscriberApplicationSubscriptions(subscriberUuid, appUuid, {
                if (it.succeeded()) {
                    def futures = []
                    def subscriberSubscriptions = it.result()
                    subscriberSubscriptions.each {
                        def future = Promise.promise()
                        futures.add(future)
                        core.storage.updateArtifactSubscription(it, it, future)
                    }
                    CompositeFuture.all(futures).onComplete({
                        if (it.succeeded()) {
                            msg.reply(true)
                        } else {
                            log.error("Failed to get refresh subscriber application subscriptions", it.cause())
                            msg.fail(500, it.cause().message)
                        }
                    })
                } else {
                    log.error("Failed to refresh subscriber application subscriptions", it.cause())
                    msg.fail(500, it.cause().message)
                }
            })
        })
        vertx.eventBus().consumer(GET_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, { msg ->
            def request = msg.body() as JsonObject
            def appUuid = request.getString("app_uuid")
            def subscriberUuid = request.getString("subscriber_uuid")

            core.storage.getSubscriberApplicationSubscriptions(subscriberUuid, appUuid, {
                if (it.succeeded()) {
                    msg.reply(new JsonArray(Json.encode(it.result())))
                } else {
                    log.error("Failed to get subscriber application subscriptions", it.cause())
                    msg.fail(500, it.cause().message)
                }
            })
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void removeInactiveArtifactSubscriptions() {
        log.debug("Removing inactivate artifact subscriptions")
        def inactiveLimit = config().getJsonObject("core").getInteger("subscription_inactive_limit_minutes")
        core.storage.getSubscriberArtifactSubscriptions({
            if (it.succeeded()) {
                def futures = []
                it.result().each { sub ->
                    if (Duration.between(sub.value, Instant.now()).toMinutes() > inactiveLimit) {
                        def future = Promise.promise()
                        futures.add(future)
                        core.storage.deleteArtifactSubscription(sub.key, future)
                    }
                }
                CompositeFuture.all(futures).onComplete({
                    if (it.failed()) {
                        it.cause().printStackTrace()
                        log.error("Failed to remove inactive artifact subscriptions", it.cause())
                    }
                })
            } else {
                it.cause().printStackTrace()
                log.error("Failed to remove inactive artifact subscriptions", it.cause())
            }
        })
    }

    private void subscribeToArtifact(Message<ArtifactSubscribeRequest> request) {
        //create artifact if doesn't exist then subscribe
        core.artifactAPI.getSourceArtifact(request.body().appUuid(), request.body().artifactQualifiedName(), {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    acceptArtifactSubscription(request)
                } else {
                    def artifact = SourceArtifact.builder()
                            .appUuid(request.body().appUuid())
                            .artifactQualifiedName(request.body().artifactQualifiedName()).build()
                    core.artifactAPI.createOrUpdateSourceArtifact(artifact, {
                        if (it.succeeded()) {
                            acceptArtifactSubscription(request)
                        } else {
                            log.error("Failed to create source artifact", it.cause())
                            request.fail(500, it.cause().message)
                        }
                    })
                }
            } else {
                log.error("Failed to get source artifact", it.cause())
                request.fail(500, it.cause().message)
            }
        })
    }

    private void acceptArtifactSubscription(Message<ArtifactSubscribeRequest> request) {
        ArtifactSubscribeRequest subRequest = request.body()
        log.info("Accepted artifact subscription: " + subRequest)

        switch (subRequest.type) {
            case SourceArtifactSubscriptionType.METRICS:
                vertx.eventBus().request(SUBSCRIBE_TO_ARTIFACT_METRICS, subRequest, {
                    request.reply(it.result().body())
                })
                break
            case SourceArtifactSubscriptionType.TRACES:
                vertx.eventBus().request(SUBSCRIBE_TO_ARTIFACT_TRACES, subRequest, {
                    request.reply(it.result().body())
                })
                break
            default:
                request.fail(500, "Unknown subscription type: " + subRequest.type)
                throw new IllegalStateException("Unknown subscription type: " + subRequest.type)
        }
    }

    private void unsubscribeFromArtifact(Message<ArtifactUnsubscribeRequest> request) {
        def unsubRequest = request.body()
        log.info("Accepted artifact unsubscription: " + unsubRequest)

        if (unsubRequest instanceof ArtifactMetricUnsubscribeRequest) {
            vertx.eventBus().request(UNSUBSCRIBE_FROM_ARTIFACT_METRICS, unsubRequest, {
                if (it.succeeded()) {
                    request.reply(true)
                } else {
                    log.error("Failed to unsubscribe from artifact metrics", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        } else if (unsubRequest instanceof ArtifactTraceUnsubscribeRequest) {
            vertx.eventBus().request(UNSUBSCRIBE_FROM_ARTIFACT_TRACES, unsubRequest, {
                if (it.succeeded()) {
                    request.reply(true)
                } else {
                    log.error("Failed to unsubscribe from artifact traces", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        } else if (unsubRequest instanceof SourceArtifactUnsubscribeRequest) {
            core.storage.getSubscriberArtifactSubscriptions(unsubRequest.subscriberUuid(), unsubRequest.appUuid(),
                    unsubRequest.artifactQualifiedName(), {
                if (it.succeeded()) {
                    if (it.result().isEmpty()) {
                        request.reply(true)
                    } else {
                        def futures = []
                        it.result().each {
                            def promise = Promise.promise()
                            futures.add(promise)
                            core.storage.deleteArtifactSubscription(it, promise)
                        }
                        CompositeFuture.all(futures).onComplete({
                            if (it.succeeded()) {
                                request.reply(true)
                            } else {
                                log.error("Failed to unsubscribe from artifact", it.cause())
                                request.fail(500, it.cause().message)
                            }
                        })
                    }
                } else {
                    log.error("Failed to unsubscribe from artifact", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        } else {
            throw new IllegalArgumentException("Invalid unsubscribe request: " + unsubRequest)
        }
    }
}
