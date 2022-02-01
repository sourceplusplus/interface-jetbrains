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

import com.intellij.ui.scale.ScaleContext
import com.intellij.util.SVGLoader
import com.intellij.util.ui.JBImageIcon
import spp.jetbrains.sourcemarker.PluginIcons
import java.io.ByteArrayInputStream
import javax.swing.Icon

/**
 * Defines the various visual icons SourceMarker may display.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkerIcons {

    val exclamationTriangle = PluginIcons.exclamationTriangle
    val performanceRamp = PluginIcons.performanceRamp
    val activeException = PluginIcons.activeException
    val LIVE_METER_COUNT_ICON = PluginIcons.count
    val LIVE_METER_GAUGE_ICON = PluginIcons.gauge
    val LIVE_METER_HISTOGRAM_ICON = PluginIcons.histogram
    val LIVE_BREAKPOINT_ACTIVE_ICON = PluginIcons.Breakpoint.active
    val LIVE_BREAKPOINT_DISABLED_ICON = PluginIcons.Breakpoint.disabled
    val LIVE_BREAKPOINT_COMPLETE_ICON = PluginIcons.Breakpoint.complete
    val LIVE_BREAKPOINT_PENDING_ICON = PluginIcons.Breakpoint.pending
    val LIVE_BREAKPOINT_ERROR_ICON = PluginIcons.Breakpoint.error

    fun getNumericGutterMarkIcon(value: Int, color: String = "#182d34"): Icon {
        return JBImageIcon(
            SVGLoader.loadHiDPI(
                null,
                ByteArrayInputStream(NumericSvgIcon(value, color).toString().toByteArray()),
                ScaleContext.create()
            )
        )
    }
}
