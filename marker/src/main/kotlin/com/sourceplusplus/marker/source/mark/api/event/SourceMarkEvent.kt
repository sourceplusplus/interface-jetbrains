package com.sourceplusplus.marker.source.mark.api.event

import com.sourceplusplus.marker.source.mark.api.SourceMark

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SourceMarkEvent(
    val sourceMark: SourceMark,
    val eventCode: IEventCode,
    vararg val params: Any
) {

    override fun toString(): String {
        return if (params.isEmpty()) {
            "Event: $eventCode - Source: $sourceMark"
        } else {
            "Event: $eventCode - Source: $sourceMark - Params: $params"
        }
    }
}
