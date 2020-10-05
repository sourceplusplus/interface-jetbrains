package com.sourceplusplus.marker.source.mark.api.event

import com.sourceplusplus.marker.source.mark.api.SourceMark

/**
 * Used to give each [SourceMark] event a unique code.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface IEventCode {
    fun code(): Int
}
