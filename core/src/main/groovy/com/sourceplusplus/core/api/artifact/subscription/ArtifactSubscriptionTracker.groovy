package com.sourceplusplus.core.api.artifact.subscription

import com.google.common.collect.Maps
import com.sourceplusplus.api.model.application.SourceApplicationSubscription
import com.sourceplusplus.api.model.artifact.*
import com.sourceplusplus.api.model.internal.ApplicationArtifact
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest
import com.sourceplusplus.api.model.trace.ArtifactTraceUnsubscribeRequest
import com.sourceplusplus.core.api.artifact.ArtifactAPI
import com.sourceplusplus.core.api.metric.track.MetricSubscriptionTracker
import com.sourceplusplus.core.api.trace.track.TraceSubscriptionTracker
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * todo: description
 *
 * @version 0.1.2
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
    private static final Map<ApplicationArtifact, Map<String, SubscriberAccess>> artifactSubscriptions =
            new ConcurrentHashMap<>()
    private final ArtifactAPI artifactAPI

    ArtifactSubscriptionTracker(ArtifactAPI artifactAPI) {
        this.artifactAPI = artifactAPI
    }

    @Override
    void start() throws Exception {
        vertx.setPeriodic(TimeUnit.SECONDS.toMillis(5), {
            vertx.eventBus().publish(UPDATE_ARTIFACT_SUBSCRIPTIONS, true)
        })
        if (config().getJsonObject("core").getInteger("subscription_inactive_limit") > 0) {
            vertx.setPeriodic(TimeUnit.SECONDS.toMillis(10), {
                pruneOldArtifactSubscriptions()
            })
        }

        vertx.eventBus().consumer(SUBSCRIBE_TO_ARTIFACT, {
            subscribeToArtifact(it as Message<ArtifactSubscribeRequest>)
        })
        vertx.eventBus().consumer(UNSUBSCRIBE_FROM_ARTIFACT, {
            unsubscribeFromArtifact(it)
        })
        vertx.eventBus().consumer(GET_ARTIFACT_SUBSCRIPTIONS, {
            def appArtifact = it.body() as ApplicationArtifact
            def subscribers = artifactSubscriptions.get(appArtifact)

            def rtnList = new ArrayList<SourceArtifactSubscriber>()
            subscribers.each {
                rtnList.add(SourceArtifactSubscriber.builder().subscriberUuid(it.key)
                        .putAllSubscriptionLastAccessed(it.value.subscriptionAccess).build())
            }
            it.reply(Json.encode(rtnList))
        })
        vertx.eventBus().consumer(GET_APPLICATION_SUBSCRIPTIONS, {
            def appUuid = it.body() as String
            def rtnList = new ArrayList<SourceApplicationSubscription>()
            artifactSubscriptions.each {
                if (appUuid == it.key.appUuid()) {
                    def appSubscription = SourceApplicationSubscription.builder()
                            .artifactQualifiedName(it.key.artifactQualifiedName())
                            .subscribers(it.value.size())
                    it.value.values().each {
                        appSubscription.addAllTypes(it.subscriptionAccess.keySet())
                    }
                    rtnList.add(appSubscription.build())
                }
            }
            it.reply(Json.encode(rtnList))
        })
        vertx.eventBus().consumer(REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, {
            def request = it.body() as JsonObject
            def appUuid = request.getString("app_uuid")
            def subscriberUuid = request.getString("subscriber_uuid")
            artifactSubscriptions.each {
                if (appUuid == it.key.appUuid()) {
                    def subscriberSubscriptions = it.value.get(subscriberUuid)
                    subscriberSubscriptions.each {
                        def access = it.subscriptionAccess
                        access.each {
                            access.put(it.key, Instant.now())
                        }
                    }
                }
            }
            it.reply(true)
        })
        vertx.eventBus().consumer(GET_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS, {
            def request = it.body() as JsonObject
            def appUuid = request.getString("app_uuid")
            def subscriberUuid = request.getString("subscriber_uuid")

            def subscriberSubscriptions = new ArrayList<SubscriberSourceArtifactSubscription>()
            artifactSubscriptions.each { sub ->
                if (appUuid == sub.key.appUuid()) {
                    sub.value.get(subscriberUuid).each {
                        subscriberSubscriptions.add(SubscriberSourceArtifactSubscription.builder()
                                .artifactQualifiedName(sub.key.artifactQualifiedName())
                                .subscriptionAccess(it.subscriptionAccess).build())
                    }
                }
            }
            it.reply(new JsonArray(Json.encode(subscriberSubscriptions)))
        })
        log.info("{} started", getClass().getSimpleName())
    }

    private void pruneOldArtifactSubscriptions() {
        def inactiveLimit = config().getJsonObject("core").getInteger("subscription_inactive_limit")
        boolean prunedData = false
        def pruneArtifacts = new ArrayList<ApplicationArtifact>()
        artifactSubscriptions.each { //iterate artifacts
            def pruneSubscribers = new ArrayList<String>()
            it.value.each { //iterate artifact subscribers
                def pruneSubscriptionAccessType = new ArrayList<SourceArtifactSubscriptionType>()
                it.value.subscriptionAccess.each { //iterate subscriber subscriptions
                    if (Duration.between(it.value, Instant.now()).toMinutes() >= inactiveLimit) {
                        pruneSubscriptionAccessType.add(it.key)
                    }
                }
                pruneSubscriptionAccessType.each { prune ->
                    it.value.subscriptionAccess.remove(prune)
                    prunedData = true
                }

                if (it.value.subscriptionAccess.isEmpty()) {
                    //subscriber has no subscriptions to artifact
                    pruneSubscribers.add(it.key)
                }
            }
            pruneSubscribers.each { prune ->
                it.value.remove(prune)
                prunedData = true
            }

            if (it.value.isEmpty()) {
                //artifact has no subscribers
                pruneArtifacts.add(it.key)
            } else if (prunedData) {
                //artifact has subscribers but maybe not for each subscription type
                def hasMetricSubscribers = it.value.find {
                    it.value.subscriptionAccess.containsKey(SourceArtifactSubscriptionType.METRICS)
                } as boolean
                if (!hasMetricSubscribers) {
                    //remove artifact metric subscription
                    def request = ArtifactMetricUnsubscribeRequest.builder()
                            .appUuid(it.key.appUuid())
                            .artifactQualifiedName(it.key.artifactQualifiedName())
                            .removeAllArtifactMetricSubscriptions(true)
                            .build()
                    vertx.eventBus().send(MetricSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT_METRICS, request)
                }

                def hasTraceSubscribers = it.value.find {
                    it.value.subscriptionAccess.containsKey(SourceArtifactSubscriptionType.TRACES)
                } as boolean
                if (!hasTraceSubscribers) {
                    //remove artifact trace subscription
                    def request = ArtifactTraceUnsubscribeRequest.builder()
                            .appUuid(it.key.appUuid())
                            .artifactQualifiedName(it.key.artifactQualifiedName())
                            .removeAllArtifactTraceSubscriptions(true)
                            .build()
                    vertx.eventBus().send(TraceSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT_TRACES, request)
                }
            }
        }
        pruneArtifacts.each {
            artifactSubscriptions.remove(it)

            //remove artifact metric subscription
            def unsubMetrics = ArtifactMetricUnsubscribeRequest.builder()
                    .appUuid(it.appUuid())
                    .artifactQualifiedName(it.artifactQualifiedName())
                    .removeAllArtifactMetricSubscriptions(true)
                    .build()
            vertx.eventBus().send(MetricSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT_METRICS, unsubMetrics)

            //remove artifact trace subscription
            def unsubTraces = ArtifactTraceUnsubscribeRequest.builder()
                    .appUuid(it.appUuid())
                    .artifactQualifiedName(it.artifactQualifiedName())
                    .removeAllArtifactTraceSubscriptions(true)
                    .build()
            vertx.eventBus().send(TraceSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT_TRACES, unsubTraces)
        }
    }

    private void subscribeToArtifact(Message<ArtifactSubscribeRequest> request) {
        //create artifact if doesn't exist then subscribe
        artifactAPI.getSourceArtifact(request.body().appUuid(), request.body().artifactQualifiedName(), {
            if (it.succeeded()) {
                if (it.result().isPresent()) {
                    acceptArtifactSubscription(request)
                } else {
                    def artifact = SourceArtifact.builder()
                            .appUuid(request.body().appUuid())
                            .artifactQualifiedName(request.body().artifactQualifiedName()).build()
                    artifactAPI.createOrUpdateSourceArtifact(artifact, {
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
        def appArtifact = ApplicationArtifact.builder()
                .appUuid(request.body().appUuid())
                .artifactQualifiedName(request.body().artifactQualifiedName()).build()
        artifactSubscriptions.putIfAbsent(appArtifact, Maps.newConcurrentMap())
        artifactSubscriptions.get(appArtifact).putIfAbsent(request.body().subscriberClientId, new SubscriberAccess())
        artifactSubscriptions.get(appArtifact).get(request.body().subscriberClientId)
                .subscriptionAccess.put(request.body().type, Instant.now())

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
    }

    private void unsubscribeFromArtifact(Message<Object> request) {
        def unsubRequest = request.body()
        if (unsubRequest instanceof ArtifactMetricUnsubscribeRequest) {
            def subscriberUuid = unsubRequest.subscriberClientId
            def appArtifact = ApplicationArtifact.builder()
                    .appUuid(unsubRequest.appUuid())
                    .artifactQualifiedName(unsubRequest.artifactQualifiedName()).build()

            vertx.eventBus().send(MetricSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT_METRICS, unsubRequest, {
                if (it.succeeded()) {
                    def removeArtifactSubscriber = it.result().body()
                    if (removeArtifactSubscriber) {
                        def artifactSubscribers = artifactSubscriptions.get(appArtifact)
                        def artifactSubscriptionAccess = artifactSubscribers?.get(subscriberUuid)?.subscriptionAccess
                        if (artifactSubscriptionAccess != null) {
                            artifactSubscriptionAccess.remove(SourceArtifactSubscriptionType.METRICS)
                        }
                        if (artifactSubscriptionAccess.isEmpty()) {
                            artifactSubscribers.remove(subscriberUuid)
                            if (artifactSubscribers.isEmpty()) {
                                artifactSubscriptions.remove(appArtifact)
                            }
                        }
                    }
                    request.reply(removeArtifactSubscriber)
                } else {
                    log.error("Failed to unsubscribe from artifact metrics", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        } else if (unsubRequest instanceof ArtifactTraceUnsubscribeRequest) {
            def subscriberUuid = unsubRequest.subscriberClientId
            def appArtifact = ApplicationArtifact.builder()
                    .appUuid(unsubRequest.appUuid())
                    .artifactQualifiedName(unsubRequest.artifactQualifiedName()).build()

            vertx.eventBus().send(TraceSubscriptionTracker.UNSUBSCRIBE_FROM_ARTIFACT_TRACES, unsubRequest, {
                if (it.succeeded()) {
                    def removeArtifactSubscriber = it.result().body()
                    if (removeArtifactSubscriber) {
                        def artifactSubscribers = artifactSubscriptions.get(appArtifact)
                        def artifactSubscriptionAccess = artifactSubscribers?.get(subscriberUuid)?.subscriptionAccess
                        if (artifactSubscriptionAccess != null) {
                            artifactSubscriptionAccess.remove(SourceArtifactSubscriptionType.TRACES)
                        }
                        if (artifactSubscriptionAccess.isEmpty()) {
                            artifactSubscribers.remove(subscriberUuid)
                            if (artifactSubscribers.isEmpty()) {
                                artifactSubscriptions.remove(appArtifact)
                            }
                        }
                    }
                    request.reply(removeArtifactSubscriber)
                } else {
                    log.error("Failed to unsubscribe from artifact traces", it.cause())
                    request.fail(500, it.cause().message)
                }
            })
        } else if (unsubRequest instanceof SourceArtifactUnsubscribeRequest) {
            def appArtifact = ApplicationArtifact.builder()
                    .appUuid(unsubRequest.appUuid())
                    .artifactQualifiedName(unsubRequest.artifactQualifiedName()).build()

            def artifactSubscribers = artifactSubscriptions.get(appArtifact)
            artifactSubscribers?.get(unsubRequest.subscriberClientId)?.subscriptionAccess?.clear()
            request.reply(true)
        } else {
            throw new IllegalArgumentException("Invalid unsubscribe request: " + unsubRequest)
        }
    }

    private static final class SubscriberAccess {
        Map<SourceArtifactSubscriptionType, Instant> subscriptionAccess = Maps.newConcurrentMap()
    }
}
