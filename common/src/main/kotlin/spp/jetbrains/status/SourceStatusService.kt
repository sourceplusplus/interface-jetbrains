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
package spp.jetbrains.status

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair
import spp.protocol.platform.general.Service

/**
 * Service for getting the current status of the Source++ plugin.
 *
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface SourceStatusService {
    companion object {
        val KEY = Key.create<SourceStatusService>("SPP_SOURCE_STATUS_SERVICE")

        @JvmStatic
        fun getInstance(project: Project): SourceStatusService {
            return project.getUserData(KEY)!!
        }

        @JvmStatic
        fun getCurrentService(project: Project): Service? {
            return getInstance(project).getCurrentService()
        }

        @JvmStatic
        fun subscribe(project: Project, listener: SourceStatusListener) {
            project.messageBus.connect().subscribe(SourceStatusListener.TOPIC, listener)
        }
    }

    fun isReady(): Boolean
    fun isConnected(): Boolean
    fun isLoggedIn(): Boolean

    fun getCurrentStatus(): Pair<SourceStatus, String?>
    fun update(status: SourceStatus, message: String? = null)

    fun getCurrentService(): Service?
    fun setCurrentService(service: Service)
    fun setActiveServices(services: List<Service>)
    fun onServiceChange(triggerInitial: Boolean = true, listener: () -> Unit)
    fun onReadyChange(triggerInitial: Boolean = true, listener: (SourceStatus) -> Unit)
}
