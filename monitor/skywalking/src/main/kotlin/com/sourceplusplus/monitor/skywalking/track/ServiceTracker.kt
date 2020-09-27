package com.sourceplusplus.monitor.skywalking.track

import com.sourceplusplus.monitor.skywalking.SkywalkingClient
import com.sourceplusplus.monitor.skywalking.SkywalkingClient.DurationStep
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.kotlin.core.eventbus.requestAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ServiceTracker(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    var currentService: GetAllServicesQuery.Result? = null
    var activeServices: List<GetAllServicesQuery.Result> = emptyList()

    override suspend fun start() {
        launch(vertx.dispatcher()) {
            activeServices = skywalkingClient.run {
                getServices(getDuration(ZonedDateTime.now().minusMinutes(15), DurationStep.MINUTE))
            }
            vertx.eventBus().publish(activeServicesUpdatedAddress, activeServices)

            if (activeServices.isNotEmpty()) {
                currentService = activeServices[0]
                vertx.eventBus().publish(currentServiceUpdatedAddress, currentService)
            }
        }

        //async accessors
        vertx.eventBus().localConsumer<Nothing>(getCurrentServiceAddress) { it.reply(currentService) }
        vertx.eventBus().localConsumer<Nothing>(getActiveServicesAddress) { it.reply(activeServices) }
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
                .requestAwait<GetAllServicesQuery.Result?>(getCurrentServiceAddress, null)
                .body()
        }

        suspend fun getActiveServices(vertx: Vertx): List<GetAllServicesQuery.Result> {
            return vertx.eventBus()
                .requestAwait<List<GetAllServicesQuery.Result>>(getActiveServicesAddress, null)
                .body()
        }
    }
}
