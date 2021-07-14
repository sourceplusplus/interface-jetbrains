package com.sourceplusplus.marker.source.mark.gutter

import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration

/**
 * A [SourceMark] which adds visualizations in the panel to the left of source code.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface GutterMark : SourceMark {

    override val type: SourceMark.Type
        get() = SourceMark.Type.GUTTER
    override val configuration: GutterMarkConfiguration

    override fun isVisible(): Boolean
    override fun setVisible(visible: Boolean)
}
