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
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
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

    private val log = logger<ServiceBridge>()
    private var currentService: Service? = null
    private var activeServices: List<Service> = emptyList()

    override suspend fun start() {
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
        SourceStatusService.getInstance(project).update(WaitingForService)

        log.info("Attempting to set current service. Can start checker: $canStartChecker")
        activeServices = skywalkingClient.run {
            getServices(getDuration(ZonedDateTime.now().minusMinutes(15), DurationStep.MINUTE))
        }

        if (activeServices.isNotEmpty()) {
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
                    SourceStatusService.getInstance(project).apply {
                        setActiveServices(activeServices)
                        setCurrentService(currentService!!)
                        update(ServiceEstablished)
                    }
                    true
                }
            }
            if (currentService == null) {
                currentService = activeServices[0]
                log.info("Current service set to: ${currentService!!.name}")
                SourceStatusService.getInstance(project).apply {
                    setActiveServices(activeServices)
                    setCurrentService(currentService!!)
                    update(ServiceEstablished)
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
}
