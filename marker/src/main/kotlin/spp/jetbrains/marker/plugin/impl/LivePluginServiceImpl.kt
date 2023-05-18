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
package spp.jetbrains.marker.plugin.impl

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import spp.jetbrains.ScopeExtensions.safeRunBlocking
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.command.LiveCommand
import spp.jetbrains.marker.command.LiveLocationContext
import spp.jetbrains.marker.indicator.LiveIndicator
import spp.jetbrains.marker.plugin.LivePluginService
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEventListener
import spp.jetbrains.marker.source.mark.guide.GuideMark

class LivePluginServiceImpl(val project: Project) : LivePluginService {

    companion object {
        private val log = logger<LivePluginServiceImpl>()
    }

    private val commands = mutableSetOf<LiveCommand>()
    private val indicators = mutableMapOf<LiveIndicator, SourceMarkEventListener>()

    override fun reset() {
        project.getUserData(LivePluginService.KEY)?.let { liveService ->
            liveService.getRegisteredLiveCommands().forEach {
                liveService.unregisterLiveCommand(it.name)
            }
            liveService.getRegisteredLiveIndicators().forEach {
                liveService.unregisterLiveIndicator(it)
            }
        }
    }

    override fun registerLiveCommand(command: LiveCommand) {
        commands.add(command)
    }

    override fun registerLiveIndicator(indicator: LiveIndicator) {
        val eventListener = SourceMarkEventListener {
            val trigger = indicator.listenForAllEvents || indicator.listenForEvents.contains(it.eventCode)
            if (trigger && it.sourceMark is GuideMark) {
                safeRunBlocking {
                    indicator.trigger(it.sourceMark, it)
                }
            }
        }
        SourceMarker.getInstance(project).addGlobalSourceMarkEventListener(eventListener)
        indicators[indicator] = eventListener

        safeRunBlocking {
            indicator.onRegister()
        }
        log.debug("Registered indicator: $indicator - Current indicators: ${indicators.size}")
    }

    override fun unregisterLiveCommand(commandName: String) {
        commands.removeIf { it.name == commandName }
    }

    override fun unregisterLiveIndicator(indicator: LiveIndicator) {
        indicators.entries.find { it.key == indicator }?.let {
            SourceMarker.getInstance(project).removeGlobalSourceMarkEventListener(it.value)
            indicators.remove(it.key)
            Disposer.dispose(indicator)
            log.debug("Unregistered indicator: $indicator - Current indicators: ${indicators.size}")
        }
    }

    override fun getRegisteredLiveCommands(): List<LiveCommand> {
        return commands.toList()
    }

    override fun getRegisteredLiveCommands(context: LiveLocationContext): List<LiveCommand> {
        return commands.filter { it.isAvailable(context) }
    }

    override fun getRegisteredLiveIndicators(): List<LiveIndicator> {
        return indicators.keys.toList()
    }
}
