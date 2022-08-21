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
package spp.jetbrains

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.vertx.core.Vertx
import spp.jetbrains.monitor.skywalking.SkywalkingMonitorService
import spp.protocol.service.LiveInstrumentService
import spp.protocol.service.LiveService
import spp.protocol.service.LiveViewService

object UserData {

    private val VERTX = Key.create<Vertx>("SPP_VERTX")
    private val LIVE_SERVICE = Key.create<LiveService>("SPP_LIVE_SERVICE")
    private val LIVE_VIEW_SERVICE = Key.create<LiveViewService>("SPP_LIVE_VIEW_SERVICE")
    private val LIVE_INSTRUMENT_SERVICE = Key.create<LiveInstrumentService>("SPP_LIVE_INSTRUMENT_SERVICE")

    fun vertx(project: Project): Vertx {
        return project.getUserData(VERTX)!!
    }

    fun vertx(project: Project, vertx: Vertx): Vertx {
        project.putUserData(VERTX, vertx)
        return vertx
    }

    fun skywalkingMonitorService(project: Project): SkywalkingMonitorService {
        return project.getUserData(SkywalkingMonitorService.KEY)!!
    }

    fun liveService(project: Project): LiveService? {
        return project.getUserData(LIVE_SERVICE)
    }

    fun liveService(project: Project, liveService: LiveService): LiveService {
        project.putUserData(LIVE_SERVICE, liveService)
        return liveService
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
}
