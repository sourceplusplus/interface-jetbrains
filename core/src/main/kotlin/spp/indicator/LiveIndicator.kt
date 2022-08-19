/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.indicator

import com.apollographql.apollo3.exception.ApolloException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joor.Reflect
import spp.jetbrains.marker.source.mark.api.event.IEventCode
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.sourcemarker.UserData
import spp.jetbrains.sourcemarker.status.SourceStatus.ConnectionError
import spp.jetbrains.sourcemarker.status.SourceStatus.Ready
import spp.jetbrains.sourcemarker.status.SourceStatusService

@Suppress("unused")
abstract class LiveIndicator {

    companion object {
        private val log = logger<LiveIndicator>()
    }

    open val listenForAllEvents: Boolean = false
    open val listenForEvents: List<IEventCode> = emptyList()

    private var periodicTimerId = -1L
    private val project: Project
    private val vertx: Vertx

    init {
        val plugin = Reflect.on(this).get<Any>("this\$0")
        project = Reflect.on(plugin).get("project")
        vertx = UserData.vertx(project)
    }

    open suspend fun onRegister() {
        vertx.setPeriodic(5000) { timerId ->
            periodicTimerId = timerId
            GlobalScope.launch(vertx.dispatcher()) {
                if (SourceStatusService.getInstance(project).getCurrentStatus().first != Ready) {
                    log.debug("Not ready, ignoring indicator refresh")
                    return@launch
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
