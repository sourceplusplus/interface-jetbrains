/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.monitor.skywalking.service

import com.intellij.openapi.diagnostic.logger
import io.vertx.core.Future
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import spp.jetbrains.monitor.skywalking.bridge.EndpointMetricsBridge
import spp.jetbrains.monitor.skywalking.bridge.EndpointTracesBridge
import spp.jetbrains.monitor.skywalking.bridge.LogsBridge
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.monitor.skywalking.model.DurationStep
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.protocol.artifact.metrics.MetricStep
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.platform.general.Service
import spp.protocol.service.LiveViewService
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscriberAddress
import spp.protocol.view.HistoricalView
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewEvent
import spp.protocol.view.rule.LiveViewRule
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Emulates a subscription-based [LiveViewService] for SkyWalking-standalone installations.
 */
class SWLiveViewService : CoroutineVerticle(), LiveViewService {

    companion object {
        private val formatter = DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmm")
            .toFormatter()
            .withZone(ZoneOffset.UTC)
        private val log = logger<SWLiveViewService>()
    }

    data class SWLiveView(
        val subscription: LiveView,
        val lastMetricsByTimeBucket: ConcurrentHashMap<Long, JsonArray> = ConcurrentHashMap<Long, JsonArray>()
    )

    private val developerId: String = "system"
    private val subscriptionMap = ConcurrentHashMap<String, SWLiveView>()
    private var currentService: Service? = null

    override suspend fun start() {
        vertx.setPeriodic(5_000) {
            subscriptionMap.forEach { (_, subscription) ->
                launch(vertx.dispatcher()) {
                    when (subscription.subscription.viewConfig.viewName) {
                        "LOGS" -> pullLatestLogs(subscription)
                        "TRACES" -> sendTracesSubscriptionUpdate(subscription)
                        else -> sendEndpointMetricSubscriptionUpdate(subscription)
                    }
                }
            }
        }

        currentService = ServiceBridge.getCurrentService(vertx)
        ServiceBridge.currentServiceConsumer(vertx).handler {
            currentService = it.body()
        }
    }

    private suspend fun sendTracesSubscriptionUpdate(swSubscription: SWLiveView) {
        val subscription = swSubscription.subscription
        val endpointId = subscription.entityIds.first()
        val traceResult = EndpointTracesBridge.getTraces(
            GetEndpointTraces(
                artifactQualifiedName = subscription.artifactQualifiedName,
                endpointId = endpointId,
                serviceId = currentService?.id,
                zonedDuration = ZonedDuration(
                    ZonedDateTime.now().minusSeconds(30),
                    ZonedDateTime.now().plusSeconds(30),
                    DurationStep.SECOND
                )
            ), vertx
        )

        traceResult.traces.forEach { trace ->
            val eventData = JsonObject()
                .put("type", "TRACES")
                .put("multiMetrics", false)
                .put("artifactQualifiedName", JsonObject.mapFrom(subscription.artifactQualifiedName))
                .put("entityId", endpointId)
                .put("timeBucket", formatter.format(trace.start))
                .put("trace", JsonObject.mapFrom(trace))
            val event = LiveViewEvent(
                subscription.subscriptionId!!,
                endpointId,
                subscription.artifactQualifiedName,
                formatter.format(trace.start),
                subscription.viewConfig,
                eventData.toString()
            )
            vertx.eventBus().publish(toLiveViewSubscriberAddress(developerId), JsonObject.mapFrom(event))
            vertx.eventBus().send(toLiveViewSubscriberAddress(subscription.subscriptionId!!), JsonObject.mapFrom(event))
        }
    }

    private suspend fun pullLatestLogs(swSubscription: SWLiveView) {
        val subscription = swSubscription.subscription
        val logsResult = LogsBridge.queryLogs(
            LogsBridge.GetEndpointLogs(
                serviceId = currentService?.id,
                zonedDuration = ZonedDuration(
                    ZonedDateTime.now().minusSeconds(30),
                    ZonedDateTime.now().plusSeconds(30),
                    DurationStep.SECOND
                )
            ), vertx
        )
        if (logsResult.succeeded()) {
            //todo: impl log filtering in skywalking
            //todo: send logs in batch
            logsResult.result().logs.asReversed().forEach {
                if (it.content == subscription.entityIds.first()) {
                    val viewEvent = LiveViewEvent(
                        subscription.subscriptionId!!,
                        "entityId",
                        subscription.artifactQualifiedName,
                        formatter.format(it.timestamp),
                        subscription.viewConfig,
                        JsonObject().put("log", JsonObject.mapFrom(it)).toString()
                    )
                    vertx.eventBus().publish(toLiveViewSubscriberAddress(developerId), JsonObject.mapFrom(viewEvent))
                    vertx.eventBus()
                        .send(toLiveViewSubscriberAddress(subscription.subscriptionId!!), JsonObject.mapFrom(viewEvent))
                }
            }
        } else {
            val replyException = logsResult.cause() as ReplyException
            if (replyException.failureCode() == 404) {
                log.warn("Failed to fetch logs. Service(s) unavailable")
            } else {
                log.error("Failed to fetch logs", logsResult.cause())
            }
        }
    }

    private suspend fun sendEndpointMetricSubscriptionUpdate(swSubscription: SWLiveView) {
        val subscription = swSubscription.subscription
        val lastMetricsByTimeBucket = swSubscription.lastMetricsByTimeBucket
        val endTime = ZonedDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.MINUTES) //exclusive
        val startTime = endTime.minusMinutes(3)
        val metricsRequest = GetEndpointMetrics(
            subscription.viewConfig.viewMetrics,
            subscription.entityIds.first(),
            ZonedDuration(startTime, endTime, DurationStep.MINUTE)
        )

        val metrics = EndpointMetricsBridge.getMetrics(metricsRequest, vertx)
        var i = 0
        var timeBucket = startTime
        while (timeBucket.isBefore(endTime)) {
            val values = metrics.map { it.values[i] }
            if (values.size > 1) {
                val jsonArray = JsonArray()
                subscription.viewConfig.viewMetrics.forEachIndexed { x, metricName ->
                    val metric = toMetric(subscription, metricName, timeBucket, values[x].value)
                    jsonArray.add(metric)
                }

                if (lastMetricsByTimeBucket[timeBucket.toEpochSecond()] != jsonArray) {
                    sendActivitySubscriptionUpdate(subscription, timeBucket, jsonArray)
                    lastMetricsByTimeBucket[timeBucket.toEpochSecond()] = jsonArray
                }
            } else {
                val metricName = subscription.viewConfig.viewMetrics.first()
                val metric = toMetric(subscription, metricName, timeBucket, values.first().value)
                if (lastMetricsByTimeBucket[timeBucket.toEpochSecond()] != JsonArray().add(metric)) {
                    sendActivitySubscriptionUpdate(subscription, timeBucket, metric)
                    lastMetricsByTimeBucket[timeBucket.toEpochSecond()] = JsonArray().add(metric)
                }
            }

            i++
            timeBucket = timeBucket.plusMinutes(1)
        }
    }

    private fun toMetric(
        subscription: LiveView,
        metricName: String,
        timeBucket: ZonedDateTime,
        value: Any
    ): JsonObject {
        return JsonObject()
            .put("entityId", subscription.entityIds.first())
            .put("serviceId", "todo")
            .put("value", value)
            .put("total", value)
            .apply {
                if (MetricType(metricName) == MetricType.Endpoint_SLA) {
                    put("percentage", value)
                }
            }
            .put("timeBucket", formatter.format(timeBucket))
            .put("lastUpdateTimestamp", 0)
            .put("id", "todo")
            .put("artifactQualifiedName", JsonObject.mapFrom(subscription.artifactQualifiedName))
            .put("meta", JsonObject().put("metricsName", metricName))
    }

    private fun sendActivitySubscriptionUpdate(
        subscription: LiveView,
        timeBucket: ZonedDateTime,
        value: Any
    ) {
        val event = LiveViewEvent(
            subscription.subscriptionId!!,
            subscription.entityIds.first(),
            subscription.artifactQualifiedName,
            formatter.format(timeBucket),
            subscription.viewConfig,
            value.toString()
        )
        vertx.eventBus().publish(toLiveViewSubscriberAddress(developerId), JsonObject.mapFrom(event))
        vertx.eventBus().send(toLiveViewSubscriberAddress(subscription.subscriptionId!!), JsonObject.mapFrom(event))
    }

    override fun saveRule(rule: LiveViewRule): Future<LiveViewRule> {
        return Future.failedFuture(UnsupportedOperationException())
    }

    override fun deleteRule(ruleName: String): Future<LiveViewRule?> {
        return Future.failedFuture(UnsupportedOperationException())
    }

    override fun getHistoricalMetrics(
        entityIds: List<String>,
        metricIds: List<String>,
        step: MetricStep,
        start: Instant,
        stop: Instant?
    ): Future<HistoricalView> {
        return Future.failedFuture(UnsupportedOperationException())
    }

    override fun addLiveView(subscription: LiveView): Future<LiveView> {
        val sub = SWLiveView(subscription.copy(subscriptionId = UUID.randomUUID().toString()))
        subscriptionMap[sub.subscription.subscriptionId!!] = sub
        return Future.succeededFuture(sub.subscription)
    }

    override fun updateLiveView(
        id: String,
        subscription: LiveView
    ): Future<LiveView> {
        val sub = subscriptionMap[id]
        if (sub != null) {
            subscriptionMap[id] = SWLiveView(subscription.copy(subscriptionId = id))
            return Future.succeededFuture(subscription)
        }
        return Future.failedFuture("Subscription not found")
    }

    override fun removeLiveView(id: String): Future<LiveView> {
        val sub = subscriptionMap.remove(id)
            ?: return Future.failedFuture(IllegalStateException("Invalid subscription id"))
        return Future.succeededFuture(sub.subscription)
    }

    override fun getLiveView(id: String): Future<LiveView> {
        val sub = subscriptionMap[id]
            ?: return Future.failedFuture(IllegalStateException("Invalid subscription id"))
        return Future.succeededFuture(sub.subscription)
    }

    override fun getLiveViews(): Future<List<LiveView>> {
        return Future.succeededFuture(subscriptionMap.values.map { it.subscription })
    }

    override fun clearLiveViews(): Future<List<LiveView>> {
        val subscriptions = subscriptionMap.values.toList()
        subscriptionMap.clear()
        return Future.succeededFuture(subscriptions.map { it.subscription })
    }

    override fun getLiveViewStats(): Future<JsonObject> {
        return Future.failedFuture(UnsupportedOperationException())
    }
}
