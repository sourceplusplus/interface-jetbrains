package spp.jetbrains.marker.source.mark.inlay.event

import spp.jetbrains.marker.source.mark.api.event.IEventCode
import spp.jetbrains.marker.source.mark.inlay.InlayMark

/**
 * Represents [InlayMark]-specific events.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
enum class InlayMarkEventCode(private val code: Int) : IEventCode {
    INLAY_MARK_VISIBLE(3000),
    INLAY_MARK_HIDDEN(3001),
    VIRTUAL_TEXT_UPDATED(3002);

    override fun code(): Int {
        return this.code
    }
}
