package spp.jetbrains.marker.source.mark.inlay

import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkConfiguration
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [SourceMark] which adds visualizations inside source code text.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface InlayMark : SourceMark {

    override val type: SourceMark.Type
        get() = SourceMark.Type.INLAY
    override val configuration: InlayMarkConfiguration
    val visible: AtomicBoolean

    override fun isVisible(): Boolean {
        return visible.get()
    }

    override fun setVisible(visible: Boolean) {
        val previousVisibility = this.visible.getAndSet(visible)
        if (visible && !previousVisibility) {
            triggerEvent(SourceMarkEvent(this, InlayMarkEventCode.INLAY_MARK_VISIBLE))
        } else if (!visible && previousVisibility) {
            triggerEvent(SourceMarkEvent(this, InlayMarkEventCode.INLAY_MARK_HIDDEN))
        }
    }
}
