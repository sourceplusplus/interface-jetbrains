package com.sourceplusplus.monitor.skywalking.bridge

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.SkywalkingClient.DurationStep
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ServiceBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    var currentService: GetAllServicesQuery.Result? = null
    var activeServices: List<GetAllServicesQuery.Result> = emptyList()

    override suspend fun start() {
        vertx.setPeriodic(5000) { timerId ->
            launch(vertx.dispatcher()) {
                activeServices = skywalkingClient.run {
                    getServices(getDuration(ZonedDateTime.now().minusMinutes(15), DurationStep.MINUTE))
                }

                if (activeServices.isNotEmpty()) {
                    vertx.cancelTimer(timerId)
                    vertx.eventBus().publish(activeServicesUpdatedAddress, activeServices)

                    currentService = activeServices[0]
                    vertx.eventBus().publish(currentServiceUpdatedAddress, currentService)
                }
            }
        }


        //async accessors
        vertx.eventBus().localConsumer<Boolean>(getCurrentServiceAddress) { msg ->
            if (msg.body() && currentService == null) {
                val consumer = currentServiceConsumer(vertx)
                if (currentService != null) {
                    consumer.unregister()
                    msg.reply(currentService)
                } else {
                    consumer.handler {
                        msg.reply(it.body())
                        consumer.unregister()
                    }
                }
            } else {
                msg.reply(currentService)
            }
        }
        vertx.eventBus().localConsumer<Boolean>(getActiveServicesAddress) { msg ->
            if (msg.body() && activeServices.isEmpty()) {
                val consumer = activeServicesConsumer(vertx)
                if (activeServices.isNotEmpty()) {
                    consumer.unregister()
                    msg.reply(activeServices)
                } else {
                    consumer.handler {
                        msg.reply(it.body())
                        consumer.unregister()
                    }
                }
            } else {
                msg.reply(activeServices)
            }
        }
    }

    companion object {
        private const val rootAddress = "monitor.skywalking.service"
        private const val getCurrentServiceAddress = "$rootAddress.currentService"
        private const val getActiveServicesAddress = "$rootAddress.activeServices"
        private const val currentServiceUpdatedAddress = "$rootAddress.currentService-Updated"
        private const val activeServicesUpdatedAddress = "$rootAddress.activeServices-Updated"

        fun currentServiceConsumer(vertx: Vertx): MessageConsumer<GetAllServicesQuery.Result> {
            return vertx.eventBus().localConsumer(currentServiceUpdatedAddress)
        }

        fun activeServicesConsumer(vertx: Vertx): MessageConsumer<List<GetAllServicesQuery.Result>> {
            return vertx.eventBus().localConsumer(activeServicesUpdatedAddress)
        }

        suspend fun getCurrentService(vertx: Vertx): GetAllServicesQuery.Result? {
            return vertx.eventBus()
                .request<GetAllServicesQuery.Result?>(getCurrentServiceAddress, false).await().body()
        }

        suspend fun getActiveServices(vertx: Vertx): List<GetAllServicesQuery.Result> {
            return vertx.eventBus()
                .request<List<GetAllServicesQuery.Result>>(getActiveServicesAddress, false).await().body()
        }

        suspend fun getCurrentServiceAwait(vertx: Vertx): GetAllServicesQuery.Result {
            return vertx.eventBus()
                .request<GetAllServicesQuery.Result>(getCurrentServiceAddress, true).await().body()
        }

        suspend fun getActiveServicesAwait(vertx: Vertx): List<GetAllServicesQuery.Result> {
            return vertx.eventBus()
                .request<List<GetAllServicesQuery.Result>>(getActiveServicesAddress, true).await().body()
        }
    }
}
