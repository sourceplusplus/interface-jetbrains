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
package spp.jetbrains.sourcemarker.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Pair
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import spp.jetbrains.ScopeExtensions.safeGlobalAsync
import spp.jetbrains.ScopeExtensions.safeGlobalLaunch
import spp.jetbrains.UserData
import spp.jetbrains.safeLaunch
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatus.*
import spp.jetbrains.status.SourceStatusService
import spp.protocol.platform.general.Service
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
class SourceStatusServiceImpl(override val project: Project) : SourceStatusService {

    companion object {
        private val log = logger<SourceStatusServiceImpl>()
    }

    private val statusLock = Any()
    private var status: SourceStatus = Pending
    private var message: String? = null
    private val reconnectionLock = Any()
    private var reconnectionJob: Job? = null
    private var currentService: Service? = null
    private var activeServices = listOf<Service>()
    private var initialService: String? = null

    override suspend fun start(initialService: String?) {
        this.initialService = initialService

        //attempt to set current service
        SourceStatusService.getInstance(project).update(Pending, "Setting current service")
        setCurrentService(true)
    }

    private fun periodicallyCheckForCurrentService() {
        log.info("No current service set, starting periodic check for current service")
        val vertx = UserData.vertx(project)
        vertx.setPeriodic(5000) { timerId ->
            vertx.safeLaunch {
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
        val managementService = UserData.liveManagementService(project)
        activeServices = managementService.getServices(null).await()

        if (activeServices.isNotEmpty()) {
            if (initialService != null) {
                currentService = activeServices.find { it.name == initialService }
                currentService?.let { log.info("Current service set to: ${it.name}") }

                return if (currentService == null) {
                    log.warn("No service found with name: $initialService")
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

    override fun isReady(): Boolean = getCurrentStatus().first.isReady
    override fun isConnected(): Boolean = getCurrentStatus().first.isConnected
    override fun isLoggedIn(): Boolean = UserData.hasSelfInfo(project)

    override fun getCurrentStatus(): Pair<SourceStatus, String?> {
        synchronized(statusLock) {
            return Pair(status, message)
        }
    }

    override fun update(status: SourceStatus, message: String?) {
        synchronized(statusLock) {
            val oldStatus = this.status
            if (oldStatus == Disabled && status != Enabled) {
                log.info("Ignoring status update from $oldStatus to $status")
                return@synchronized
            }

            message?.let { log.info(it) }
            this.message = message
            if (oldStatus != status) {
                if (!status.ephemeral) {
                    this.status = status
                    log.info("Status changed from $oldStatus to $status")
                }

                SourceStatusService.getInstance(project).publishStatus(status)
                safeGlobalLaunch {
                    onStatusChanged(status)
                }
            }
        }

        updateAllStatusBarIcons()
    }

    override fun getCurrentService(): Service? {
        return currentService
    }

    @Synchronized
    override fun setCurrentService(service: Service) {
        currentService = service
        update(ServiceEstablished)
    }

    @Synchronized
    override fun setActiveServices(services: List<Service>) {
        activeServices = services.toMutableList()
    }

    override fun onServiceChange(triggerInitial: Boolean, listener: () -> Unit) {
        if (triggerInitial && currentService != null) {
            synchronized(statusLock) {
                listener()
            }
        }
        UserData.vertx(project).eventBus().consumer<SourceStatus>("spp.status") {
            if (it.body() == ServiceEstablished) {
                synchronized(statusLock) {
                    listener()
                }
            }
        }
    }

    override fun onStatusChange(triggerInitial: Boolean, listener: (SourceStatus) -> Unit) {
        synchronized(statusLock) {
            if (triggerInitial) {
                synchronized(statusLock) {
                    listener(status)
                }
            }
            if (UserData.hasVertx(project)) {
                UserData.vertx(project).eventBus().consumer<SourceStatus>("spp.status") {
                    synchronized(statusLock) {
                        listener(it.body())
                    }
                }
            } else {
                log.warn("No vertx instance found for project: $project")
            }
        }
    }

    override fun onReadyChange(triggerInitial: Boolean, listener: (SourceStatus) -> Unit) {
        if (triggerInitial) {
            synchronized(statusLock) {
                listener(status)
            }
        }

        val statusTriggered = AtomicBoolean()
        UserData.vertx(project).eventBus().consumer<SourceStatus>("spp.status") {
            synchronized(statusLock) {
                if (isReady() && statusTriggered.compareAndSet(false, true)) {
                    listener(status)
                } else if (!isReady() && statusTriggered.compareAndSet(true, false)) {
                    listener(status)
                }
            }
        }
    }

    private suspend fun onStatusChanged(status: SourceStatus) = when (status) {
        ConnectionError -> {
            SourceMarkerPlugin.getInstance(project).disposePlugin()

            //start reconnection loop
            synchronized(reconnectionLock) {
                reconnectionJob = launchPeriodicInit(15_000, true)
            }
        }

        Enabled -> {
            SourceMarkerPlugin.getInstance(project).init()
        }

        Disabled -> {
            SourceMarkerPlugin.getInstance(project).disposePlugin()
            stopReconnectionLoop()
        }

        else -> {
            stopReconnectionLoop()
        }
    }

    private fun stopReconnectionLoop() {
        synchronized(reconnectionLock) {
            reconnectionJob?.cancel()
            reconnectionJob = null
        }
    }

    private fun updateAllStatusBarIcons() {
        val action = Runnable {
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) {
                    SourceStatusBarWidget.update(project)
                }
            }
        }
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            action.run()
        } else {
            application.invokeLater(action)
        }
    }

    private fun launchPeriodicInit(
        repeatMillis: Long,
        waitBefore: Boolean
    ) = safeGlobalAsync {
        while (isActive) {
            if (waitBefore) delay(repeatMillis)
            if (project.isDisposed) {
                log.info("${project.name} is disposed, stopping reconnection loop")
                break
            } else {
                SourceMarkerPlugin.getInstance(project).init()
            }
            if (!waitBefore) delay(repeatMillis)
        }
    }
}
