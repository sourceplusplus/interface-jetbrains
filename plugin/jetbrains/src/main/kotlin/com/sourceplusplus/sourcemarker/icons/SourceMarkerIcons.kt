package com.sourceplusplus.sourcemarker.icons

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.SVGLoader
import com.intellij.util.ui.JBImageIcon
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.advice.cautionary.RampDetectionAdvice
import com.sourceplusplus.protocol.advice.informative.ActiveExceptionAdvice
import com.sourceplusplus.protocol.icon.NumericSvgIcon
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
    val EYE_ICON = IconLoader.getIcon("/icons/eye.svg")
    val GREY_EYE_ICON = IconLoader.getIcon("/icons/eye-grey.svg")
    val GREEN_EYE_ICON = IconLoader.getIcon("/icons/eye-green.svg")
    val YELLOW_EYE_ICON = IconLoader.getIcon("/icons/eye-yellow.svg")
    val EYE_SLASH_ICON = IconLoader.getIcon("/icons/eye-slash.svg")

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
