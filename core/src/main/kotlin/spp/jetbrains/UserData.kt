/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.Vertx
import spp.protocol.platform.developer.SelfInfo
import spp.protocol.service.LiveInstrumentService
import spp.protocol.service.LiveManagementService
import spp.protocol.service.LiveViewService

object UserData {

    private val VERTX = Key.create<Vertx>("SPP_VERTX")
    private val SELF_INFO = Key.create<SelfInfo>("SPP_SELF_INFO")
    private val LIVE_MANAGEMENT_SERVICE = Key.create<LiveManagementService>("SPP_LIVE_MANAGEMENT_SERVICE")
    private val LIVE_VIEW_SERVICE = Key.create<LiveViewService>("SPP_LIVE_VIEW_SERVICE")
    private val LIVE_INSTRUMENT_SERVICE = Key.create<LiveInstrumentService>("SPP_LIVE_INSTRUMENT_SERVICE")

    fun vertx(project: Project): Vertx {
        return project.getUserData(VERTX)!!
    }

    fun vertx(project: Project, vertx: Vertx): Vertx {
        project.putUserData(VERTX, vertx)
        return vertx
    }

    fun hasVertx(project: Project): Boolean {
        return project.getUserData(VERTX) != null
    }

    fun hasLiveManagementService(project: Project): Boolean {
        return project.getUserData(LIVE_MANAGEMENT_SERVICE) != null
    }

    fun liveManagementService(project: Project): LiveManagementService {
        return project.getUserData(LIVE_MANAGEMENT_SERVICE)!!
    }

    fun liveManagementService(project: Project, liveManagementService: LiveManagementService): LiveManagementService {
        project.putUserData(LIVE_MANAGEMENT_SERVICE, liveManagementService)
        return liveManagementService
    }

    fun liveViewService(project: Project): LiveViewService? {
        return project.getUserData(LIVE_VIEW_SERVICE)
    }

    fun liveViewService(project: Project, liveViewService: LiveViewService): LiveViewService {
        project.putUserData(LIVE_VIEW_SERVICE, liveViewService)
        return liveViewService
    }

    @JvmStatic
    fun liveInstrumentService(project: Project): LiveInstrumentService? {
        return project.getUserData(LIVE_INSTRUMENT_SERVICE)
    }

    fun liveInstrumentService(project: Project, liveInstrumentService: LiveInstrumentService): LiveInstrumentService {
        project.putUserData(LIVE_INSTRUMENT_SERVICE, liveInstrumentService)
        return liveInstrumentService
    }

    fun selfInfo(project: Project): SelfInfo? {
        return project.getUserData(SELF_INFO)
    }

    fun selfInfo(project: Project, selfInfo: SelfInfo): SelfInfo {
        project.putUserData(SELF_INFO, selfInfo)
        return selfInfo
    }

    fun hasSelfInfo(project: Project): Boolean {
        return project.getUserData(SELF_INFO) != null
    }

    fun clear(project: Project) {
        project.putUserData(VERTX, null)
        project.putUserData(SELF_INFO, null)
        project.putUserData(LIVE_MANAGEMENT_SERVICE, null)
        project.putUserData(LIVE_VIEW_SERVICE, null)
        project.putUserData(LIVE_INSTRUMENT_SERVICE, null)
    }

    fun developerId(project: Project): String {
        return selfInfo(project)?.developer?.id ?: "system"
    }
}
