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
package spp.jetbrains.sourcemarker.icons

import spp.jetbrains.icons.PluginIcons

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
