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
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.SkywalkingClient.DurationStep
import spp.protocol.platform.general.Service
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class ServiceBridge(
    private val skywalkingClient: SkywalkingClient,
    private val initServiceName: String?
) : CoroutineVerticle() {

    var currentService: Service? = null
    var activeServices: List<Service> = emptyList()

    override suspend fun start() {
        vertx.setPeriodic(5000) { timerId ->
            launch(vertx.dispatcher()) {
                activeServices = skywalkingClient.run {
                    getServices(getDuration(ZonedDateTime.now().minusMinutes(15), DurationStep.MINUTE))
                }

                if (activeServices.isNotEmpty()) {
                    vertx.cancelTimer(timerId)
                    vertx.eventBus().publish(activeServicesUpdatedAddress, activeServices)

                    if (initServiceName != null) {
                        currentService = activeServices.find { it.name == initServiceName }
                    }
                    if (currentService == null) {
                        currentService = activeServices[0]
                    }
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

        fun currentServiceConsumer(vertx: Vertx): MessageConsumer<Service> {
            return vertx.eventBus().localConsumer(currentServiceUpdatedAddress)
        }

        fun activeServicesConsumer(vertx: Vertx): MessageConsumer<List<Service>> {
            return vertx.eventBus().localConsumer(activeServicesUpdatedAddress)
        }

        suspend fun getCurrentService(vertx: Vertx): Service {
            return vertx.eventBus()
                .request<Service>(getCurrentServiceAddress, true).await().body()
        }

        suspend fun getActiveServices(vertx: Vertx): List<Service> {
            return vertx.eventBus()
                .request<List<Service>>(getActiveServicesAddress, true).await().body()
        }
    }
}
