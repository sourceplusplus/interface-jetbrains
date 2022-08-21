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
package spp.jetbrains.icons

import com.intellij.openapi.util.IconLoader

object PluginIcons {
    object Nodes {
        @JvmField
        val variable = IconLoader.getIcon("/nodes/variable.png", PluginIcons::class.java)
    }

    object Command {
        @JvmField
        val logo = IconLoader.getIcon("/icons/command/logo.svg", PluginIcons::class.java)

        @JvmField
        val clearInstrumentSelected =
            IconLoader.getIcon("/icons/command/clear-instruments_selected.svg", PluginIcons::class.java)

        @JvmField
        val clearInstrumentUnSelected =
            IconLoader.getIcon("/icons/command/clear-instruments_unselected.svg", PluginIcons::class.java)

        @JvmField
        val liveBreakpointSelected =
            IconLoader.getIcon("/icons/command/live-breakpoint_selected.svg", PluginIcons::class.java)

        @JvmField
        val liveBreakpointUnSelected =
            IconLoader.getIcon("/icons/command/live-breakpoint_unselected.svg", PluginIcons::class.java)

        @JvmField
        val liveLogSelected = IconLoader.getIcon("/icons/command/live-log_selected.svg", PluginIcons::class.java)

        @JvmField
        val liveLogUnSelected =
            IconLoader.getIcon("/icons/command/live-log_unselected.svg", PluginIcons::class.java)

        @JvmField
        val liveMeterSelected =
            IconLoader.getIcon("/icons/command/live-meter_selected.svg", PluginIcons::class.java)

        @JvmField
        val liveMeterUnSelected =
            IconLoader.getIcon("/icons/command/live-meter_unselected.svg", PluginIcons::class.java)

        @JvmField
        val liveSpanSelected = IconLoader.getIcon("/icons/command/live-span_selected.svg", PluginIcons::class.java)

        @JvmField
        val liveSpanUnSelected =
            IconLoader.getIcon("/icons/command/live-span_unselected.svg", PluginIcons::class.java)

        @JvmField
        val viewOverviewSelected =
            IconLoader.getIcon("/icons/command/view-overview_selected.svg", PluginIcons::class.java)

        @JvmField
        val viewOverviewUnSelected =
            IconLoader.getIcon("/icons/command/view-overview_unselected.svg", PluginIcons::class.java)

        @JvmField
        val viewActivitySelected =
            IconLoader.getIcon("/icons/command/view-activity_selected.svg", PluginIcons::class.java)

        @JvmField
        val viewActivityUnSelected =
            IconLoader.getIcon("/icons/command/view-activity_unselected.svg", PluginIcons::class.java)

        @JvmField
        val viewTracesSelected =
            IconLoader.getIcon("/icons/command/view-traces_selected.svg", PluginIcons::class.java)

        @JvmField
        val viewTracesUnSelected =
            IconLoader.getIcon("/icons/command/view-traces_unselected.svg", PluginIcons::class.java)

        @JvmField
        val viewLogsSelected = IconLoader.getIcon("/icons/command/view-logs_selected.svg", PluginIcons::class.java)

        @JvmField
        val viewLogsUnSelected =
            IconLoader.getIcon("/icons/command/view-logs_unselected.svg", PluginIcons::class.java)

        @JvmField
        val quickStatsSelected =
            IconLoader.getIcon("/icons/command/quick-stats_selected.svg", PluginIcons::class.java)

        @JvmField
        val quickStatsUnSelected =
            IconLoader.getIcon("/icons/command/quick-stats_unselected.svg", PluginIcons::class.java)

        @JvmField
        val watchLogSelected = IconLoader.getIcon("/icons/command/watch-log_selected.svg", PluginIcons::class.java)

        @JvmField
        val watchLogUnSelected =
            IconLoader.getIcon("/icons/command/watch-log_unselected.svg", PluginIcons::class.java)
    }

    object Instrument {
        @JvmField
        val save = IconLoader.getIcon("/icons/instrument/live-log/save.svg", PluginIcons::class.java)

        @JvmField
        val saveHovered = IconLoader.getIcon("/icons/instrument/live-log/saveHovered.svg", PluginIcons::class.java)

        @JvmField
        val savePressed = IconLoader.getIcon("/icons/instrument/live-log/savePressed.svg", PluginIcons::class.java)
    }

    object Breakpoint {
        @JvmField
        val active = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-active.svg", PluginIcons::class.java)

        @JvmField
        val complete = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-complete.svg", PluginIcons::class.java)

        @JvmField
        val disabled = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-disabled.svg", PluginIcons::class.java)

        @JvmField
        val error = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-error.svg", PluginIcons::class.java)

        @JvmField
        val pending = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-pending.svg", PluginIcons::class.java)
    }

    @JvmField
    val statusEnabled = IconLoader.getIcon("/icons/statusBar/status-enabled.svg", PluginIcons::class.java)

    @JvmField
    val statusPending = IconLoader.getIcon("/icons/statusBar/status-pending.svg", PluginIcons::class.java)

    @JvmField
    val statusDisabled = IconLoader.getIcon("/icons/statusBar/status-disabled.svg", PluginIcons::class.java)

    @JvmField
    val statusFailed = IconLoader.getIcon("/icons/statusBar/status-failed.svg", PluginIcons::class.java)

    @JvmField
    val expand = IconLoader.getIcon("/icons/expand.svg", PluginIcons::class.java)

    @JvmField
    val expandHovered = IconLoader.getIcon("/icons/expandHovered.svg", PluginIcons::class.java)

    @JvmField
    val expandPressed = IconLoader.getIcon("/icons/expandPressed.svg", PluginIcons::class.java)

    @JvmField
    val close = IconLoader.getIcon("/icons/closeIcon.svg", PluginIcons::class.java)

    @JvmField
    val closeHovered = IconLoader.getIcon("/icons/closeIconHovered.svg", PluginIcons::class.java)

    @JvmField
    val closePressed = IconLoader.getIcon("/icons/closeIconPressed.svg", PluginIcons::class.java)

    @JvmField
    val clock = IconLoader.getIcon("/icons/clock.svg", PluginIcons::class.java)

    @JvmField
    val logConfig = IconLoader.getIcon("/icons/log-config.svg", PluginIcons::class.java)

    @JvmField
    val angleDown = IconLoader.getIcon("/icons/angle-down.svg", PluginIcons::class.java)

    @JvmField
    val breakpointConfig = IconLoader.getIcon("/icons/breakpoint-config.svg", PluginIcons::class.java)

    @JvmField
    val eyeSlash = IconLoader.getIcon("/icons/eye-slash.svg", PluginIcons::class.java)

    @JvmField
    val meterConfig = IconLoader.getIcon("/icons/meter-config.svg", PluginIcons::class.java)

    @JvmField
    val spanConfig = IconLoader.getIcon("/icons/span-config.svg", PluginIcons::class.java)

    @JvmField
    val config = IconLoader.getIcon("/icons/configIcon.svg", PluginIcons::class.java)

    @JvmField
    val configHovered = IconLoader.getIcon("/icons/configIconHovered.svg", PluginIcons::class.java)

    @JvmField
    val configPressed = IconLoader.getIcon("/icons/configIconPressed.svg", PluginIcons::class.java)

    @JvmField
    val exclamationTriangle = IconLoader.getIcon("/icons/exclamation-triangle.svg", PluginIcons::class.java)

    @JvmField
    val performanceRamp = IconLoader.getIcon("/icons/sort-amount-up.svg", PluginIcons::class.java)

    @JvmField
    val activeException = IconLoader.getIcon("/icons/map-marker-exclamation.svg", PluginIcons::class.java)

    @JvmField
    val count = IconLoader.getIcon("/icons/count.svg", PluginIcons::class.java)

    @JvmField
    val gauge = IconLoader.getIcon("/icons/gauge.svg", PluginIcons::class.java)

    @JvmField
    val histogram = IconLoader.getIcon("/icons/histogram.svg", PluginIcons::class.java)

    @JvmField
    val code = IconLoader.getIcon("/icons/code.svg", PluginIcons::class.java)

    @JvmField
    val tachometer = IconLoader.getIcon("/icons/tachometer-alt.svg", PluginIcons::class.java)
}