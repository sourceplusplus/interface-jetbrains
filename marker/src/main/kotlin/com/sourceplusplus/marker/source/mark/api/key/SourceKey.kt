package com.sourceplusplus.marker.source.mark.api.key

import com.intellij.openapi.util.Key
import com.sourceplusplus.marker.source.mark.gutter.GutterMark
import com.sourceplusplus.marker.source.mark.inlay.InlayMark

/**
 * Used to associate custom data to PSI elements.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class SourceKey<T>(val name: String) {
    companion object {
        @JvmField
        val GutterMark = Key.create<GutterMark>("sm.GutterMark")

        @JvmField
        val InlayMark = Key.create<InlayMark>("sm.InlayMark")
    }
}
