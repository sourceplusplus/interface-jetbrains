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

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.monitor.skywalking.SkywalkingClient
import spp.jetbrains.monitor.skywalking.model.DurationStep
import spp.jetbrains.status.SourceStatus.*
import spp.jetbrains.status.SourceStatusService
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
    private val project: Project,
    private val skywalkingClient: SkywalkingClient,
    private val initServiceName: String?
) : CoroutineVerticle() {

    private var currentService: Service? = null
    private var activeServices: List<Service> = emptyList()

    override suspend fun start() {
        //async accessors
        vertx.eventBus().localConsumer<Boolean>(getCurrentServiceAddress) { msg ->
            if (currentService == null) {
                log.warn("No current service set")
            }
            msg.reply(currentService)
        }
        vertx.eventBus().localConsumer<Boolean>(getActiveServicesAddress) { msg ->
            if (activeServices.isEmpty()) {
                log.warn("No active services set")
            }
            msg.reply(activeServices)
        }

        //attempt to set current service
        SourceStatusService.getInstance(project).update(Pending, "Setting current service")
        setCurrentService(true)
    }

    private fun periodicallyCheckForCurrentService() {
        log.info("No current service set, starting periodic check for current service")
        vertx.setPeriodic(5000) { timerId ->
            launch(vertx.dispatcher()) {
                try {
                    if (setCurrentService(false)) {
                        vertx.cancelTimer(timerId)
                    }
                } catch (e: Exception) {
                    log.warn("Error setting current service", e)
                }
            }
        }
    }

    private suspend fun setCurrentService(canStartChecker: Boolean): Boolean {
        log.info("Attempting to set current service. Can start checker: $canStartChecker")
        activeServices = skywalkingClient.run {
            getServices(getDuration(ZonedDateTime.now().minusMinutes(15), DurationStep.MINUTE))
        }

        if (activeServices.isNotEmpty()) {
            vertx.eventBus().publish(activeServicesUpdatedAddress, activeServices)

            if (initServiceName != null) {
                currentService = activeServices.find { it.name == initServiceName }
                currentService?.let { log.info("Current service set to: ${it.name}") }

                return if (currentService == null) {
                    log.warn("No service found with name: $initServiceName")
                    if (canStartChecker) {
                        periodicallyCheckForCurrentService()
                    }
                    false
                } else {
                    vertx.eventBus().publish(currentServiceUpdatedAddress, currentService)
                    SourceStatusService.getInstance(project).apply {
                        setActiveServices(activeServices)
                        setCurrentService(currentService!!)
                        update(ServiceChange)
                        update(Ready)
                    }
                    true
                }
            }
            if (currentService == null) {
                currentService = activeServices[0]
                log.info("Current service set to: ${currentService!!.name}")
                vertx.eventBus().publish(currentServiceUpdatedAddress, currentService)
                SourceStatusService.getInstance(project).apply {
                    setActiveServices(activeServices)
                    setCurrentService(currentService!!)
                    update(ServiceChange)
                    update(Ready)
                }
                return true
            }
        } else {
            log.warn("No active services found")
            if (canStartChecker) {
                periodicallyCheckForCurrentService()
            }
        }
        return false
    }

    companion object {
        private val log = logger<ServiceBridge>()

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

        suspend fun getCurrentService(vertx: Vertx): Service? {
            return vertx.eventBus()
                .request<Service>(getCurrentServiceAddress, true).await().body()
        }

        suspend fun getActiveServices(vertx: Vertx): List<Service> {
            return vertx.eventBus()
                .request<List<Service>>(getActiveServicesAddress, true).await().body()
        }

        fun getCurrentServiceAwait(vertx: Vertx): Service? {
            return safeRunBlocking(vertx.dispatcher()) {
                vertx.eventBus()
                    .request<Service>(getCurrentServiceAddress, true).await().body()
            }
        }
    }
}
