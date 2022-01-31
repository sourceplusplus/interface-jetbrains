/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
