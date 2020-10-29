package com.sourceplusplus.sourcemarker

import com.intellij.openapi.util.IconLoader
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.sourcemarker.listeners.PluginSourceMarkEventListener

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
}
