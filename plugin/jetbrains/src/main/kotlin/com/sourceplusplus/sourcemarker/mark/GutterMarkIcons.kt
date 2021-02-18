package com.sourceplusplus.sourcemarker.mark

import com.intellij.openapi.util.IconLoader
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.protocol.advice.cautionary.RampDetectionAdvice
import com.sourceplusplus.protocol.advice.informative.ActiveExceptionAdvice
import com.sourceplusplus.sourcemarker.listeners.PluginSourceMarkEventListener
import javax.swing.Icon

/**
 * Defines the various visual icons [GutterMark]s may display.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object GutterMarkIcons {
    val exclamationTriangle = IconLoader.getIcon(
        "/icons/exclamation-triangle.svg",
        PluginSourceMarkEventListener::class.java
    )
    val performanceRamp = IconLoader.getIcon(
        "/icons/sort-amount-up.svg",
        PluginSourceMarkEventListener::class.java
    )
    val activeException = IconLoader.getIcon(
        "/icons/map-marker-exclamation.svg",
        PluginSourceMarkEventListener::class.java
    )

    fun getGutterMarkIcon(advice: ArtifactAdvice): Icon? {
        return when (advice) {
            is ActiveExceptionAdvice -> exclamationTriangle
            is RampDetectionAdvice -> performanceRamp
            else -> null
        }
    }
}
