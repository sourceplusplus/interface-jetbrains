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
package spp.jetbrains.monitor.skywalking.bridge

import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import monitor.skywalking.protocol.metadata.GetServiceInstancesQuery
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.model.DurationStep
import java.time.ZonedDateTime

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
class ServiceInstanceBridge(private val skywalkingClient: SkywalkingClient) : CoroutineVerticle() {

    var currentServiceInstance: GetServiceInstancesQuery.Result? = null
    var activeServicesInstances: List<GetServiceInstancesQuery.Result> = emptyList()

    override suspend fun start() {
        //update active/current service instances on service update
        ServiceBridge.currentServiceConsumer(vertx).handler {
            launch(vertx.dispatcher()) {
                activeServicesInstances = skywalkingClient.run {
                    getServiceInstances(
                        it.body().id,
                        //todo: dynamic duration
                        getDuration(ZonedDateTime.now().minusMinutes(15), DurationStep.MINUTE)
                    )
                }
                vertx.eventBus().publish(activeServiceInstancesUpdatedAddress, activeServicesInstances)

                if (activeServicesInstances.isNotEmpty()) {
                    currentServiceInstance = activeServicesInstances[0]
                    vertx.eventBus().publish(currentServiceInstanceUpdatedAddress, currentServiceInstance)
                }
            }
        }
        vertx.eventBus().localConsumer<String>(getServiceInstances) {
            launch(vertx.dispatcher()) {
                val servicesInstances = skywalkingClient.run {
                    getServiceInstances(
                        it.body(),
                        //todo: dynamic duration
                        getDuration(ZonedDateTime.now().minusMinutes(15), DurationStep.MINUTE)
                    )
                }
                it.reply(servicesInstances)
            }
        }

        //async accessors
        vertx.eventBus().localConsumer<Nothing>(getCurrentServiceInstanceAddress) { it.reply(currentServiceInstance) }
        vertx.eventBus().localConsumer<Nothing>(getActiveServiceInstancesAddress) { it.reply(activeServicesInstances) }
    }

    companion object {
        private const val rootAddress = "monitor.skywalking.service.instance"
        private const val getServiceInstances = "monitor.skywalking.service.instance-serviceInstances"
        private const val getCurrentServiceInstanceAddress = "$rootAddress.currentServiceInstance"
        private const val getActiveServiceInstancesAddress = "$rootAddress.activeServiceInstances"
        private const val currentServiceInstanceUpdatedAddress = "$rootAddress.currentServiceInstance-Updated"
        private const val activeServiceInstancesUpdatedAddress = "$rootAddress.activeServiceInstances-Updated"

        fun currentServiceInstanceConsumer(vertx: Vertx): MessageConsumer<GetServiceInstancesQuery.Result> {
            return vertx.eventBus().localConsumer(currentServiceInstanceUpdatedAddress)
        }

        fun activeServiceInstancesConsumer(vertx: Vertx): MessageConsumer<List<GetServiceInstancesQuery.Result>> {
            return vertx.eventBus().localConsumer(activeServiceInstancesUpdatedAddress)
        }

        suspend fun getCurrentServiceInstance(vertx: Vertx): GetServiceInstancesQuery.Result? {
            return vertx.eventBus()
                .request<GetServiceInstancesQuery.Result?>(getCurrentServiceInstanceAddress, null)
                .await().body()
        }

        suspend fun getActiveServiceInstances(vertx: Vertx): List<GetServiceInstancesQuery.Result> {
            return vertx.eventBus()
                .request<List<GetServiceInstancesQuery.Result>>(getActiveServiceInstancesAddress, null)
                .await().body()
        }

        suspend fun getServiceInstances(serviceId: String, vertx: Vertx): List<GetServiceInstancesQuery.Result> {
            return vertx.eventBus()
                .request<List<GetServiceInstancesQuery.Result>>(getServiceInstances, serviceId)
                .await().body()
        }
    }
}
