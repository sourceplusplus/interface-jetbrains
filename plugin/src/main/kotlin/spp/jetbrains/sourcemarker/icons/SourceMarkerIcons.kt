package spp.jetbrains.sourcemarker.icons

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.SVGLoader
import com.intellij.util.ui.JBImageIcon
import spp.jetbrains.sourcemarker.PluginIcons
import spp.protocol.advice.ArtifactAdvice
import spp.protocol.advice.cautionary.RampDetectionAdvice
import spp.protocol.advice.informative.ActiveExceptionAdvice
import java.io.ByteArrayInputStream
import javax.swing.Icon

/**
 * Defines the various visual icons SourceMarker may display.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkerIcons {

    val exclamationTriangle = PluginIcons.exclmationTriangle
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

    fun getGutterMarkIcon(advice: ArtifactAdvice): Icon? {
        return when (advice) {
            is ActiveExceptionAdvice -> exclamationTriangle
            is RampDetectionAdvice -> performanceRamp
            else -> null
        }
    }

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
