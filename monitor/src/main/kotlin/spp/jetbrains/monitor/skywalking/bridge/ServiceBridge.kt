/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
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
