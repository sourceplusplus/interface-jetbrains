package com.sourceplusplus.marker.source.mark.inlay.event

import com.sourceplusplus.marker.source.mark.api.event.IEventCode

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class InlayMarkEventCode(private val code: Int) : IEventCode {
    VIRTUAL_TEXT_UPDATED(3000);

    override fun code(): Int {
        return this.code
    }
}
