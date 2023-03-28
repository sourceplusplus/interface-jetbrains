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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import spp.jetbrains.ScopeExtensions.safeGlobalAsync
import spp.jetbrains.ScopeExtensions.safeGlobalLaunch
import spp.jetbrains.UserData
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.status.SourceStatus
import spp.jetbrains.status.SourceStatus.*
import spp.jetbrains.status.SourceStatusListener
import spp.jetbrains.status.SourceStatusService
import spp.protocol.platform.general.Service
import java.util.concurrent.atomic.AtomicBoolean

class SourceStatusServiceImpl(val project: Project) : SourceStatusService {

    companion object {
        private val log = logger<SourceStatusServiceImpl>()
    }

    private val statusLock = Any()
    private var status: SourceStatus = Pending
    private var message: String? = null
    private val reconnectionLock = Any()
    private var reconnectionJob: Job? = null
    private var currentService: Service? = null
    private var activeServices = mutableListOf<Service>()

    override fun isReady(): Boolean {
        return getCurrentStatus().first.isReady
    }

    override fun isConnected(): Boolean {
        return getCurrentStatus().first.isConnected
    }

    override fun isLoggedIn(): Boolean {
        return UserData.hasSelfInfo(project)
    }

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

            if (oldStatus != status) {
                if (status != ServiceChange) {
                    this.status = status
                    this.message = message
                    log.info("Status changed from $oldStatus to $status")
                }

                project.messageBus.syncPublisher(SourceStatusListener.TOPIC).onStatusChanged(status)
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
        update(ServiceChange)
    }

    @Synchronized
    override fun setActiveServices(services: List<Service>) {
        activeServices = services.toMutableList()
    }

    override fun onServiceChange(triggerInitial: Boolean, listener: () -> Unit) {
        synchronized(statusLock) {
            if (triggerInitial && currentService != null) {
                listener()
            }
            project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
                if (it == ServiceChange) {
                    listener()
                }
            })
        }
    }

    override fun onReadyChange(triggerInitial: Boolean, listener: () -> Unit) {
        synchronized(statusLock) {
            if (triggerInitial) {
                listener()
            }

            val statusTriggered = AtomicBoolean()
            project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, SourceStatusListener {
                if (isReady() && statusTriggered.compareAndSet(false, true)) {
                    listener()
                } else if (!isReady() && statusTriggered.compareAndSet(true, false)) {
                    listener()
                }
            })
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
