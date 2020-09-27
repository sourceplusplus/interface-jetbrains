package com.sourceplusplus.marker.source.mark.gutter

import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import org.slf4j.LoggerFactory

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface GutterMark : SourceMark {

    companion object {
        private val log = LoggerFactory.getLogger(GutterMark::class.java)
    }

    override val type: SourceMark.Type
        get() = SourceMark.Type.GUTTER
    override val configuration: GutterMarkConfiguration

    fun isVisible(): Boolean
    fun setVisible(visible: Boolean)
}
