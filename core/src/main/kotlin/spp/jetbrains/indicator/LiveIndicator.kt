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
package spp.jetbrains.indicator

import com.apollographql.apollo3.exception.ApolloException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import spp.jetbrains.UserData
import spp.jetbrains.marker.source.mark.api.event.IEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.plugin.LiveStatusManager
import spp.jetbrains.safeLaunch
import spp.jetbrains.status.SourceStatus.ConnectionError
import spp.jetbrains.status.SourceStatusService
import spp.protocol.platform.developer.SelfInfo

@Suppress("unused")
abstract class LiveIndicator(val project: Project) {

    val log by lazy { Logger.getInstance("spp.jetbrains.indicator." + this::class.java.simpleName) }
    open val listenForAllEvents: Boolean = false
    open val listenForEvents: List<IEventCode> = emptyList()

    private var periodicTimerId = -1L
    val vertx = UserData.vertx(project)
    val skywalkingMonitorService = UserData.skywalkingMonitorService(project)
    val managementService = UserData.liveManagementService(project)!!
    val viewService = UserData.liveViewService(project)!!
    val statusManager = LiveStatusManager.getInstance(project)
    val instrumentService = UserData.liveInstrumentService(project)
    val selfInfo: SelfInfo
        get() = UserData.selfInfo(project)

    open suspend fun onRegister() {
        vertx.setPeriodic(5000) { timerId ->
            periodicTimerId = timerId
            vertx.safeLaunch {
                if (!SourceStatusService.getInstance(project).isReady()) {
                    log.debug("Not ready, ignoring indicator refresh")
                    return@safeLaunch
                }

                try {
                    refreshIndicator()
                } catch (ex: ApolloException) {
                    log.warn("Error refreshing indicator", ex)
                    SourceStatusService.getInstance(project)
                        .update(ConnectionError, "Unable to connect to platform")
                }
            }
        }
    }

    open suspend fun onUnregister() {
        vertx.cancelTimer(periodicTimerId)
    }

    open suspend fun refreshIndicator() = Unit
    open suspend fun trigger(guideMark: GuideMark, event: SourceMarkEvent) = Unit
}
