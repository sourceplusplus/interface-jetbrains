package com.sourceplusplus.sourcemarker.icons

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.SVGLoader
import com.intellij.util.ui.JBImageIcon
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.advice.cautionary.RampDetectionAdvice
import com.sourceplusplus.protocol.advice.informative.ActiveExceptionAdvice
import java.io.ByteArrayInputStream
import javax.swing.Icon

/**
 * Defines the various visual icons SourceMarker may display.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceMarkerIcons {

    val exclamationTriangle = IconLoader.getIcon("/icons/exclamation-triangle.svg")
    val performanceRamp = IconLoader.getIcon("/icons/sort-amount-up.svg")
    val activeException = IconLoader.getIcon("/icons/map-marker-exclamation.svg")
    val LIVE_METER_ICON = IconLoader.getIcon("/icons/tally.svg")
    val LIVE_BREAKPOINT_ACTIVE_ICON = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-active.svg")
    val LIVE_BREAKPOINT_DISABLED_ICON = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-disabled.svg")
    val LIVE_BREAKPOINT_COMPLETE_ICON = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-complete.svg")
    val LIVE_BREAKPOINT_PENDING_ICON = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-pending.svg")
    val LIVE_BREAKPOINT_ERROR_ICON = IconLoader.getIcon("/icons/breakpoint/live-breakpoint-error.svg")

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
