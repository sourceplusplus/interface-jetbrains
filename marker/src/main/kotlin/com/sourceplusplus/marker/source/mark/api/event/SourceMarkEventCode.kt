package com.sourceplusplus.marker.source.mark.api.event

import com.sourceplusplus.marker.source.mark.api.SourceMark

/**
 * Represents general [SourceMark] events.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
enum class SourceMarkEventCode(private val code: Int) : IEventCode {
    MARK_ADDED(1000),
    MARK_REMOVED(1001),
    NAME_CHANGED(1002);

    override fun code(): Int {
        return this.code
    }
}
