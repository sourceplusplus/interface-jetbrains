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
package spp.jetbrains.sourcemarker.stat

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Pair
import kotlinx.coroutines.*
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.status.SourceStatus
import spp.jetbrains.sourcemarker.status.SourceStatusService
import spp.jetbrains.sourcemarker.statusBar.SourceStatusBarWidget
import java.lang.Runnable
import javax.annotation.concurrent.GuardedBy

class SourceStatusServiceImpl(val project: Project) : SourceStatusService {

    private val statusLock = Any()

    @GuardedBy("statusLock")
    private var status: SourceStatus = SourceStatus.Pending

    @GuardedBy("statusLock")
    private var message: String? = null

    private val reconnectionLock = Any()

    @GuardedBy("reconnectionLock")
    private var reconnectionJob: Job? = null

    override fun getCurrentStatus(): Pair<SourceStatus, String?> {
        synchronized(statusLock) {
            return Pair(status, message)
        }
    }

    override fun update(status: SourceStatus, message: String?) {
        synchronized(statusLock) {
            val oldStatus = this.status
            this.status = status
            this.message = message

            if (oldStatus != status) {
                onStatusChanged(status)
            }
        }

        updateAllStatusBarIcons()
    }

    private fun onStatusChanged(status: SourceStatus) = when (status) {
        SourceStatus.ConnectionError -> {
            //start reconnection loop
            synchronized(reconnectionLock) {
                reconnectionJob = launchPeriodicAsync(15_000)
            }
        }

        else -> {
            synchronized(reconnectionLock) {
                reconnectionJob?.cancel()
                reconnectionJob = null
            }
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

    private fun launchPeriodicAsync(
        repeatMillis: Long
    ) = GlobalScope.async {
        while (isActive) {
            delay(repeatMillis)
            SourceMarkerPlugin.getInstance(project).init()
        }
    }
}