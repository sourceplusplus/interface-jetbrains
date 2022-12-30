/*
 * Source++, the continuous feedback platform for developers.
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

/**
 * Defines the various visual icons Source++ may display.
 *
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object PluginIcons {
    object Nodes {
        @JvmField
        val variable = IconLoader.getIcon("/nodes/variable.png", PluginIcons::class.java)
    }

    object Command {
        @JvmField
        val logo = IconLoader.getIcon("/icons/command/logo.svg", PluginIcons::class.java)
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
    val count = IconLoader.getIcon("/icons/count.svg", PluginIcons::class.java)

    @JvmField
    val gauge = IconLoader.getIcon("/icons/gauge.svg", PluginIcons::class.java)

    @JvmField
    val histogram = IconLoader.getIcon("/icons/histogram.svg", PluginIcons::class.java)

    @JvmField
    val server = IconLoader.getIcon("/icons/server.svg", PluginIcons::class.java)

    @JvmField
    val chartArea = IconLoader.getIcon("/icons/chart-area.svg", PluginIcons::class.java)

    @JvmField
    val earthAmericas = IconLoader.getIcon("/icons/earth-americas.svg", PluginIcons::class.java)

    @JvmField
    val diagramSubtask = IconLoader.getIcon("/icons/diagram-subtask.svg", PluginIcons::class.java)

    @JvmField
    val squareCheck = IconLoader.getIcon("/icons/square-check.svg", PluginIcons::class.java)

    @JvmField
    val squareDashed = IconLoader.getIcon("/icons/square-dashed.svg", PluginIcons::class.java)
}
