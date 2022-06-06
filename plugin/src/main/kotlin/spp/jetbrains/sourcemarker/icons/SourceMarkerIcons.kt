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
package spp.jetbrains.sourcemarker.icons

import spp.jetbrains.sourcemarker.PluginIcons

/**
 * Defines the various visual icons SourceMarker may display.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkerIcons {

    val LIVE_METER_COUNT_ICON = PluginIcons.count
    val LIVE_METER_GAUGE_ICON = PluginIcons.gauge
    val LIVE_METER_HISTOGRAM_ICON = PluginIcons.histogram
    val LIVE_BREAKPOINT_ACTIVE_ICON = PluginIcons.Breakpoint.active
    val LIVE_BREAKPOINT_DISABLED_ICON = PluginIcons.Breakpoint.disabled
}
