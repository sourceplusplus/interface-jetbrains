package com.sourceplusplus.core.api.artifact.subscription

import com.sourceplusplus.api.model.artifact.*
import com.sourceplusplus.api.model.internal.ApplicationArtifact
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest
import com.sourceplusplus.api.model.trace.ArtifactTraceUnsubscribeRequest
import com.sourceplusplus.core.SourceCore
import com.sourceplusplus.core.api.metric.track.MetricSubscriptionTracker
import com.sourceplusplus.core.api.trace.track.TraceSubscriptionTracker
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * todo: description
 *
 * @version 0.2.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class ArtifactSubscriptionTracker extends AbstractVerticle {

    public static final String SUBSCRIBE_TO_ARTIFACT = "SubscribeToArtifact"
    public static final String UNSUBSCRIBE_FROM_ARTIFACT = "UnsubscribeFromArtifact"
    public static final String UPDATE_ARTIFACT_SUBSCRIPTIONS = "UpdateArtifactSubscriptions"
    public static final String GET_ARTIFACT_SUBSCRIPTIONS = "GetArtifactSubscriptions"
    public static final String GET_APPLICATION_SUBSCRIPTIONS = "GetApplicationSubscriptions"
    public static final String REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS = "RefreshSubscriberApplicationSubscriptions"
    public static final String GET_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS = "GetSubscriberApplicationSubscriptions"

    private static final Logger log = LoggerFactory.getLogger(this.name)
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
            vertx.eventBus().publish(UPDATE_ARTIFACT_SUBSCRIPTIONS, true)
        })
        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT, {
            subscribeToArtifact(it as Message<ArtifactSubscribeRequest>)
        })
        vertx.eventBus().consumer(UNSUBSCRIBE_FROM_ARTIFACT, {
            unsubscribeFromArtifact(it)
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
        vertx.eventBus().consumer(GET_APPLICATION_SUBSCRIPTIONS, { msg ->
            def appUuid = msg.body() as String
            core.storage.getApplicationSubscriptions(appUuid, {
                if (it.succeeded()) {
                    msg.reply(Json.encode(it.result()))
                } else {
                    log.error("Failed to get application subscriptions", it.cause())
                    msg.fail(500, it.cause().message)
                }
            })
        })
        vertx.eventBus().consumer(REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, { msg ->
            def request = msg.body() as JsonObject
            def appUuid = request.getString("app_uuid")
            def subscriberUuid = request.getString("subscriber_uuid")

            core.storage.getSubscriberArtifactSubscriptions(subscriberUuid, appUuid, {
                if (it.succeeded()) {
                    def futures = []
                    def subscriberSubscriptions = it.result()
                    subscriberSubscriptions.each {
                        def updatedAccess = new HashMap<>(it.subscriptionLastAccessed())
                        updatedAccess.each {
                            def now = Instant.now()
                            updatedAccess.put(it.key, now)
                        }

                        def future = Future.future()
                        futures.add(future)
                        core.storage.setArtifactSubscription(it.withSubscriptionLastAccessed(updatedAccess), future.completer())
                    }
                    CompositeFuture.all(futures).setHandler({
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

            core.storage.getSubscriberArtifactSubscriptions(subscriberUuid, appUuid, {
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
        core.storage.getArtifactSubscriptions({
            if (it.succeeded()) {
                def futures = []
                it.result().each { sub ->
                    def updated = false
                    def updatedSubscriptions = new HashMap<>(sub.subscriptionLastAccessed())
                    sub.subscriptionLastAccessed().each {
                        if (Duration.between(it.value, Instant.now()).toMinutes() > inactiveLimit) {
                            updatedSubscriptions.remove(it.key)
                            updated = true
                        }
                    }

                    def future = Future.future()
                    futures.add(future)
                    if (updatedSubscriptions.isEmpty()) {
                        core.storage.deleteArtifactSubscription(sub, future.completer())
                    } else if (updated) {
                        sub = sub.withSubscriptionLastAccessed(updatedSubscriptions)
                        core.storage.setArtifactSubscription(sub, future.completer())
                    }
                }
                CompositeFuture.all(futures).setHandler({
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
        log.info("Accepted artifact subscription: " + request.body())
        def artifactSubscription = SourceArtifactSubscription.builder()
                .subscriberUuid(request.body().subscriberClientId)
                .appUuid(request.body().appUuid())
                .artifactQualifiedName(request.body().artifactQualifiedName())
                .putSubscriptionLastAccessed(request.body().type, Instant.now())
                .build()
        core.storage.updateArtifactSubscription(artifactSubscription, {
            if (it.succeeded()) {
                switch (request.body().type) {
                    case SourceArtifactSubscriptionType.METRICS:
                        vertx.eventBus().send(MetricSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT_METRICS, request.body(), {
                            request.reply(it.result().body())
                        })
                        break
                    case SourceArtifactSubscriptionType.TRACES:
                        vertx.eventBus().send(TraceSubscriptionTracker.SUBSCRIBE_TO_ARTIFACT_TRACES, request.body(), {
                            request.reply(it.result().body())
                        })
                        break
                    default:
                        throw new IllegalStateException("Unknown subscription type: " + request.body().type)
                }
            } else {
                log.error("Failed to add artifact subscription", it.cause())
                request.fail(500, it.cause().message)
            }
        })
    }

    private void unsubscribeFromArtifact(Message<Object> request) {
        def unsubRequest = request.body()
        if (unsubRequest instanceof ArtifactMetricUnsubscribeRequest) {
            def metricUnsubRequest = unsubRequest
            vertx.eventBus().send(MetricSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT_METRICS, unsubRequest, {
                if (it.succeeded()) {
                    def removeArtifactSubscriber = it.result().body()
                    if (removeArtifactSubscriber) {
                        core.storage.getArtifactSubscription(metricUnsubRequest.subscriberClientId,
                                metricUnsubRequest.appUuid(), metricUnsubRequest.artifactQualifiedName(), {
                            if (it.succeeded()) {
                                if (it.result().isPresent()) {
                                    def subscription = it.result().get()
                                    def updatedAccess = new HashMap<>(subscription.subscriptionLastAccessed())
                                    updatedAccess.remove(SourceArtifactSubscriptionType.METRICS)

                                    if (updatedAccess.isEmpty()) {
                                        core.storage.deleteArtifactSubscription(subscription, {
                                            if (it.succeeded()) {
                                                request.reply(true)
                                            } else {
                                                log.error("Failed to unsubscribe from artifact metrics", it.cause())
                                                request.fail(500, it.cause().message)
                                            }
                                        })
                                    } else {
                                        subscription = subscription.withSubscriptionLastAccessed(updatedAccess)
                                        core.storage.setArtifactSubscription(subscription, {
                                            if (it.succeeded()) {
                                                request.reply(true)
                                            } else {
                                                log.error("Failed to unsubscribe from artifact metrics", it.cause())
                                                request.fail(500, it.cause().message)
                                            }
                                        })
                                    }
                                } else {
                                    request.reply(true)
                                }
                            } else {
                                log.error("Failed to unsubscribe from artifact metrics", it.cause())
                                request.fail(500, it.cause().message)
                            }
                        })
                    } else {
                        request.reply(true)
                    }
                } else {
                    log.error("Failed to unsubscribe from artifact metrics", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        } else if (unsubRequest instanceof ArtifactTraceUnsubscribeRequest) {
            def traceUnsubRequest = unsubRequest
            vertx.eventBus().send(TraceSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT_TRACES, traceUnsubRequest, {
                if (it.succeeded()) {
                    def removeArtifactSubscriber = it.result().body()
                    if (removeArtifactSubscriber) {
                        core.storage.getArtifactSubscription(traceUnsubRequest.subscriberClientId,
                                traceUnsubRequest.appUuid(), traceUnsubRequest.artifactQualifiedName(), {
                            if (it.succeeded()) {
                                if (it.result().isPresent()) {
                                    def subscription = it.result().get()
                                    def updatedAccess = new HashMap<>(subscription.subscriptionLastAccessed())
                                    updatedAccess.remove(SourceArtifactSubscriptionType.TRACES)

                                    if (updatedAccess.isEmpty()) {
                                        core.storage.deleteArtifactSubscription(subscription, {
                                            if (it.succeeded()) {
                                                request.reply(true)
                                            } else {
                                                log.error("Failed to unsubscribe from artifact traces", it.cause())
                                                request.fail(500, it.cause().message)
                                            }
                                        })
                                    } else {
                                        subscription = subscription.withSubscriptionLastAccessed(updatedAccess)
                                        core.storage.setArtifactSubscription(subscription, {
                                            if (it.succeeded()) {
                                                request.reply(true)
                                            } else {
                                                log.error("Failed to unsubscribe from artifact traces", it.cause())
                                                request.fail(500, it.cause().message)
                                            }
                                        })
                                    }
                                } else {
                                    request.reply(true)
                                }
                            } else {
                                log.error("Failed to unsubscribe from artifact traces", it.cause())
                                request.fail(500, it.cause().message)
                            }
                        })
                    } else {
                        request.reply(true)
                    }
                } else {
                    log.error("Failed to unsubscribe from artifact traces", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        } else if (unsubRequest instanceof SourceArtifactUnsubscribeRequest) {
            core.storage.getArtifactSubscription(unsubRequest.subscriberClientId, unsubRequest.appUuid(),
                    unsubRequest.artifactQualifiedName(), {
                if (it.succeeded()) {
                    if (it.result().isPresent()) {
                        core.storage.deleteArtifactSubscription(it.result().get(), {
                            if (it.succeeded()) {
                                request.reply(true)
                            } else {
                                log.error("Failed to unsubscribe from artifact", it.cause())
                                request.fail(500, it.cause().message)
                            }
                        })
                    } else {
                        request.reply(true)
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
