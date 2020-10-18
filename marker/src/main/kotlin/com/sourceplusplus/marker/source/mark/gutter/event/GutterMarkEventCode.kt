package com.sourceplusplus.marker.source.mark.gutter.event

import com.sourceplusplus.marker.source.mark.api.event.IEventCode
import com.sourceplusplus.marker.source.mark.gutter.GutterMark

/**
 * Represents [GutterMark]-specific events.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
enum class GutterMarkEventCode(private val code: Int) : IEventCode {
    GUTTER_MARK_VISIBLE(2000),
    GUTTER_MARK_HIDDEN(2001);

    override fun code(): Int {
        return this.code
    }
}
