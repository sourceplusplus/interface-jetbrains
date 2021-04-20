package com.sourceplusplus.marker.source.mark.api.event

import com.sourceplusplus.marker.source.mark.api.SourceMark

/**
 * Represents general [SourceMark] events.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
enum class SourceMarkEventCode(private val code: Int) : IEventCode {
    MARK_ADDED(1000),
    MARK_BEFORE_ADDED(1001),
    MARK_REMOVED(1002),
    NAME_CHANGED(1003),
    PORTAL_OPENING(1004),
    PORTAL_OPENED(1005),
    PORTAL_CLOSED(1006);

    override fun code(): Int {
        return this.code
    }
}
