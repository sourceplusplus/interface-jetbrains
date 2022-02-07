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
package spp.jetbrains.monitor.skywalking.bridge

import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metadata.GetAllServicesQuery
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.SkywalkingClient.DurationStep
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
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

        suspend fun getCurrentService(vertx: Vertx): GetAllServicesQuery.Result {
            return vertx.eventBus()
                .request<GetAllServicesQuery.Result>(getCurrentServiceAddress, true).await().body()
        }

        suspend fun getActiveServices(vertx: Vertx): List<GetAllServicesQuery.Result> {
            return vertx.eventBus()
                .request<List<GetAllServicesQuery.Result>>(getActiveServicesAddress, true).await().body()
        }
    }
}
