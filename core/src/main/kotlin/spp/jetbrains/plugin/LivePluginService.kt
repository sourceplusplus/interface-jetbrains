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
package spp.jetbrains.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import spp.jetbrains.command.LiveCommand
import spp.jetbrains.indicator.LiveIndicator
import spp.jetbrains.marker.source.mark.api.SourceMark
import java.io.File

interface LivePluginService {
    fun reset()
    fun registerLiveCommand(command: LiveCommand)
    fun unregisterLiveCommand(commandName: String)
    fun getRegisteredLiveCommands(): List<LiveCommand>
    fun getRegisteredLiveCommands(sourceMark: SourceMark): List<LiveCommand>
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
