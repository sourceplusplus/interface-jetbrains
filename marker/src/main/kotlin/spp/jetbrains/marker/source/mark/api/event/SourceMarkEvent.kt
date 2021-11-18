package spp.jetbrains.marker.source.mark.api.event

import spp.jetbrains.marker.source.mark.api.SourceMark

/**
 * todo: description.
 *
 * @since 0.1.0
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
