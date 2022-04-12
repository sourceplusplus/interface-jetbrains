/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.monitor.skywalking.service

import io.vertx.core.Future
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaInstant
import org.slf4j.LoggerFactory
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.bridge.EndpointMetricsBridge
import spp.jetbrains.monitor.skywalking.bridge.EndpointTracesBridge
import spp.jetbrains.monitor.skywalking.bridge.LogsBridge
import spp.jetbrains.monitor.skywalking.bridge.ServiceBridge
import spp.jetbrains.monitor.skywalking.model.GetEndpointMetrics
import spp.jetbrains.monitor.skywalking.model.GetEndpointTraces
import spp.jetbrains.monitor.skywalking.model.ZonedDuration
import spp.protocol.SourceServices.Provide.toLiveViewSubscriberAddress
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.artifact.metrics.MetricType.ServiceLevelAgreement_Average
import spp.protocol.platform.general.Service
import spp.protocol.service.LiveViewService
import spp.protocol.view.LiveViewEvent
import spp.protocol.view.LiveViewSubscription
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
        private val log = LoggerFactory.getLogger(SWLiveViewService::class.java)
    }

    data class SWLiveViewSubscription(
        val subscription: LiveViewSubscription,
        val lastMetricsByTimeBucket: ConcurrentHashMap<Long, JsonArray> = ConcurrentHashMap<Long, JsonArray>()
    )

    private val developerId: String = "system"
    private val subscriptionMap = ConcurrentHashMap<String, SWLiveViewSubscription>()
    private var currentService: Service? = null

    override suspend fun start() {
        vertx.setPeriodic(5_000) {
            subscriptionMap.forEach { (_, subscription) ->
                launch(vertx.dispatcher()) {
                    when (subscription.subscription.liveViewConfig.viewName) {
                        "ACTIVITY" -> sendActivitySubscriptionUpdate(subscription)
                        "LOGS" -> pullLatestLogs(subscription)
                        "TRACES" -> sendTracesSubscriptionUpdate(subscription)
                        else -> TODO("not implemented")
                    }
                }
            }
        }

        currentService = ServiceBridge.getCurrentService(vertx)
        ServiceBridge.currentServiceConsumer(vertx).handler {
            currentService = it.body()
        }
    }

    private suspend fun sendTracesSubscriptionUpdate(swSubscription: SWLiveViewSubscription) {
        val subscription = swSubscription.subscription
        val endpointId = subscription.artifactQualifiedName.operationName!! //todo: PortalEventListener sets temp
        val traceResult = EndpointTracesBridge.getTraces(
            GetEndpointTraces(
                artifactQualifiedName = subscription.artifactQualifiedName,
                endpointId = endpointId,
                serviceId = currentService?.id,
                zonedDuration = ZonedDuration(
                    ZonedDateTime.now().minusSeconds(30),
                    ZonedDateTime.now().plusSeconds(30),
                    SkywalkingClient.DurationStep.SECOND
                )
            ), vertx
        )

        traceResult.traces.forEach { trace ->
            val eventData = JsonObject()
                .put("type", "TRACES")
                .put("multiMetrics", false)
                .put("artifactQualifiedName", JsonObject.mapFrom(subscription.artifactQualifiedName))
                .put("entityId", endpointId)
                .put("timeBucket", formatter.format(trace.start.toJavaInstant()))
                .put("trace", JsonObject.mapFrom(trace))
            val event = LiveViewEvent(
                subscription.subscriptionId!!,
                endpointId,
                subscription.artifactQualifiedName,
                formatter.format(trace.start.toJavaInstant()),
                subscription.liveViewConfig,
                eventData.toString()
            )
            vertx.eventBus().publish(toLiveViewSubscriberAddress(developerId), JsonObject.mapFrom(event))
            vertx.eventBus().send(toLiveViewSubscriberAddress(subscription.subscriptionId!!), JsonObject.mapFrom(event))
        }
    }

    private suspend fun pullLatestLogs(swSubscription: SWLiveViewSubscription) {
        val subscription = swSubscription.subscription
        val logsResult = LogsBridge.queryLogs(
            LogsBridge.GetEndpointLogs(
                serviceId = currentService?.id,
                zonedDuration = ZonedDuration(
                    ZonedDateTime.now().minusSeconds(30),
                    ZonedDateTime.now().plusSeconds(30),
                    SkywalkingClient.DurationStep.SECOND
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
                        formatter.format(it.timestamp.toJavaInstant()),
                        subscription.liveViewConfig,
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

    private suspend fun sendActivitySubscriptionUpdate(swSubscription: SWLiveViewSubscription) {
        val subscription = swSubscription.subscription
        val endpointId = subscription.artifactQualifiedName.operationName!! //todo: PortalEventListener & ActivityQuickStatsIndicator sets temp
        val lastMetricsByTimeBucket = swSubscription.lastMetricsByTimeBucket
        val endTime = ZonedDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.MINUTES) //exclusive
        val startTime = endTime.minusMinutes(3)
        val metricsRequest = GetEndpointMetrics(
            subscription.liveViewConfig.viewMetrics,
            endpointId, //subscription.entityIds.first(),
            ZonedDuration(startTime, endTime, SkywalkingClient.DurationStep.MINUTE)
        )

        val metrics = EndpointMetricsBridge.getMetrics(metricsRequest, vertx)
        var i = 0
        var timeBucket = startTime
        while (timeBucket.isBefore(endTime)) {
            val values = metrics.map { it.values[i] }
            if (values.size > 1) {
                val jsonArray = JsonArray()
                subscription.liveViewConfig.viewMetrics.forEachIndexed { x, metricName ->
                    val metric = toMetric(subscription, metricName, timeBucket, values[x].value)
                    jsonArray.add(metric)
                }

                if (lastMetricsByTimeBucket[timeBucket.toEpochSecond()] != jsonArray) {
                    sendActivitySubscriptionUpdate(subscription, timeBucket, jsonArray)
                    lastMetricsByTimeBucket[timeBucket.toEpochSecond()] = jsonArray
                }
            } else {
                val metricName = subscription.liveViewConfig.viewMetrics.first()
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
        subscription: LiveViewSubscription,
        metricName: String,
        timeBucket: ZonedDateTime,
        value: Any
    ): JsonObject {
        return JsonObject()
            .put("entityId", subscription.artifactQualifiedName.operationName) //todo: PortalEventListener sets temp
            .put("entityName", subscription.entityIds.first())
            .put("serviceId", "todo")
            .put("value", value)
            .put("total", value)
            .apply {
                if (MetricType.realValueOf(metricName) == ServiceLevelAgreement_Average) {
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
        subscription: LiveViewSubscription,
        timeBucket: ZonedDateTime,
        value: Any
    ) {
        val event = LiveViewEvent(
            subscription.subscriptionId!!,
            subscription.entityIds.first(),
            subscription.artifactQualifiedName,
            formatter.format(timeBucket),
            subscription.liveViewConfig,
            value.toString()
        )
        vertx.eventBus().publish(toLiveViewSubscriberAddress(developerId), JsonObject.mapFrom(event))
        vertx.eventBus().send(toLiveViewSubscriberAddress(subscription.subscriptionId!!), JsonObject.mapFrom(event))
    }

    override fun addLiveViewSubscription(subscription: LiveViewSubscription): Future<LiveViewSubscription> {
        val sub = SWLiveViewSubscription(subscription.copy(subscriptionId = UUID.randomUUID().toString()))
        subscriptionMap[sub.subscription.subscriptionId!!] = sub
        return Future.succeededFuture(sub.subscription)
    }

    override fun removeLiveViewSubscription(subscriptionId: String): Future<LiveViewSubscription> {
        val sub = subscriptionMap.remove(subscriptionId)
            ?: return Future.failedFuture(IllegalStateException("Invalid subscription id"))
        return Future.succeededFuture(sub.subscription)
    }

    override fun getLiveViewSubscriptions(): Future<List<LiveViewSubscription>> {
        return Future.succeededFuture(subscriptionMap.values.map { it.subscription })
    }

    override fun clearLiveViewSubscriptions(): Future<List<LiveViewSubscription>> {
        val subscriptions = subscriptionMap.values.toList()
        subscriptionMap.clear()
        return Future.succeededFuture(subscriptions.map { it.subscription })
    }

    override fun getLiveViewSubscriptionStats(): Future<JsonObject> {
        return Future.failedFuture(UnsupportedOperationException())
    }
}
