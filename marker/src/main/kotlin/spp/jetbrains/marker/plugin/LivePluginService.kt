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
package spp.jetbrains.marker.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import spp.jetbrains.marker.command.LiveCommand
import spp.jetbrains.marker.command.LiveLocationContext
import spp.jetbrains.marker.indicator.LiveIndicator
import java.io.File

interface LivePluginService {
    fun reset()
    fun registerLiveCommand(command: LiveCommand)
    fun unregisterLiveCommand(commandName: String)
    fun getRegisteredLiveCommands(): List<LiveCommand>
    fun getRegisteredLiveCommands(context: LiveLocationContext): List<LiveCommand>
    fun registerLiveIndicator(indicator: LiveIndicator)
    fun unregisterLiveIndicator(indicator: LiveIndicator)
    fun getRegisteredLiveIndicators(): List<LiveIndicator>

    companion object {
        val KEY = Key.create<LivePluginService>("SPP_LIVE_PLUGIN_SERVICE")
        val LIVE_PLUGIN_LOADER = Key.create<() -> Unit>("SPP_LIVE_PLUGIN_LOADER")
        val SPP_PLUGINS_LOCATION = Key.create<File>("SPP_PLUGINS_LOCATION")

        fun getInstance(project: Project): LivePluginService {
            return project.getUserData(KEY)!!
        }
    }
}
