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
package spp.jetbrains.sourcemarker.command

import spp.jetbrains.marker.extend.LiveCommand
import spp.jetbrains.sourcemarker.PluginBundle.message
import spp.jetbrains.sourcemarker.PluginIcons
import spp.jetbrains.sourcemarker.PluginUI.getCommandHighlightColor
import spp.jetbrains.sourcemarker.PluginUI.getCommandTypeColor
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("unused", "MaxLineLength")
class LiveControlCommand(
    val name: String,
    val command: String,
    private val description: () -> String,
    val selectedIcon: Icon? = null,
    val unselectedIcon: Icon? = null,
    val liveCommand: LiveCommand? = null
) : AutocompleteFieldRow {
    companion object {
        val SHOW_QUICK_STATS = LiveControlCommand(
            "SHOW_QUICK_STATS",
            message("show_quick_stats"),
            {
                "<html><span style=\"font-size: 80%; color: ${getCommandTypeColor()}\">" + message("live_view") + " ➛ " + message(
                    "quick_stats"
                ) + " ➛ " + message("scope") + ": </span><span style=\"font-size: 80%; color: ${getCommandHighlightColor()}\">" + message(
                    "endpoint"
                ) + "</span></html>"
            },
            PluginIcons.Command.quickStatsSelected,
            PluginIcons.Command.quickStatsUnSelected
        )
        val HIDE_QUICK_STATS = LiveControlCommand(
            "HIDE_QUICK_STATS",
            message("hide_quick_stats"),
            {
                "<html><span style=\"font-size: 80%; color: ${getCommandTypeColor()}\">" + message("live_view") + " ➛ " + message(
                    "quick_stats"
                ) + " ➛ " + message("scope") + ": </span><span style=\"font-size: 80%; color: ${getCommandHighlightColor()}\">" + message(
                    "endpoint"
                ) + "</span></html>"
            },
            PluginIcons.Command.quickStatsSelected,
            PluginIcons.Command.quickStatsUnSelected
        )
        val WATCH_LOG = LiveControlCommand(
            "WATCH_LOG",
            message("watch_log"),
            {
                "<html><span style=\"font-size: 80%; color: ${getCommandTypeColor()}\">" + message("live_view") + " ➛ " + message(
                    "log"
                ) + " ➛ " + message("scope") + ": </span><span style=\"font-size: 80%; color: ${getCommandHighlightColor()}\">" + message(
                    "Expression"
                ) + "</span></html>"
            },
            PluginIcons.Command.watchLogSelected,
            PluginIcons.Command.watchLogUnSelected
        )

        //    WATCH_VARIABLE(
        //        "watch",
        //        "Manual Tracing ➛ Watched Variables ➛ Scope: Local / Add *variable* to watched variables"
        //    ),
        //    TRACE_METHOD(
        //        "trace",
        //        "Add method to distributed tracing system"
        //    ),
        val CLEAR_LIVE_INSTRUMENTS = LiveControlCommand(
            "CLEAR_LIVE_INSTRUMENTS",
            message("clear_instruments"),
            {
                "<html><span style=\"font-size: 80%; color: ${getCommandTypeColor()}\">" + message("live_instrument") + " ➛ " + message(
                    "clear_all"
                ) + "</span></html>"
            },
            PluginIcons.Command.clearInstrumentSelected,
            PluginIcons.Command.clearInstrumentUnSelected
        )
        val CLEAR_LIVE_BREAKPOINTS = LiveControlCommand(
            "CLEAR_LIVE_BREAKPOINTS",
            message("clear_breakpoints"),
            { "Clear all self-created live breakpoints" },
            PluginIcons.Command.clearInstrumentSelected,
            PluginIcons.Command.clearInstrumentUnSelected
        )
        val CLEAR_LIVE_LOGS = LiveControlCommand(
            "CLEAR_LIVE_LOGS",
            message("clear_logs"),
            { "Clear all self-created live logs" },
            PluginIcons.Command.clearInstrumentSelected,
            PluginIcons.Command.clearInstrumentUnSelected
        )
        val CLEAR_LIVE_METERS = LiveControlCommand(
            "CLEAR_LIVE_METERS",
            message("clear_meters"),
            { "Clear all self-created live meters" },
            PluginIcons.Command.clearInstrumentSelected,
            PluginIcons.Command.clearInstrumentUnSelected
        )
        val CLEAR_LIVE_SPANS = LiveControlCommand(
            "CLEAR_LIVE_SPANS",
            message("clear_spans"),
            { "Clear all self-created live spans" },
            PluginIcons.Command.clearInstrumentSelected,
            PluginIcons.Command.clearInstrumentUnSelected
        )

        fun values() = arrayOf(
            SHOW_QUICK_STATS,
            HIDE_QUICK_STATS,
            WATCH_LOG,
            CLEAR_LIVE_INSTRUMENTS,
            CLEAR_LIVE_BREAKPOINTS,
            CLEAR_LIVE_LOGS,
            CLEAR_LIVE_METERS,
            CLEAR_LIVE_SPANS
        )
    }

    override fun getText(): String {
        return command
    }

    override fun getDescription(): String {
        return description.invoke()
    }

    override fun getIcon(): Icon? {
        return unselectedIcon
    }
}
