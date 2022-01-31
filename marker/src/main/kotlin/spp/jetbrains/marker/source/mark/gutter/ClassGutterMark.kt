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
package spp.jetbrains.marker.source.mark.gutter

import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.SourceMarkerVisibilityAction
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ClassSourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.gutter.config.GutterMarkConfiguration
import spp.jetbrains.marker.source.mark.gutter.event.GutterMarkEventCode
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a [GutterMark] associated to a class artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class ClassGutterMark(
    override val sourceFileMarker: SourceFileMarker,
    override var psiClass: PsiNameIdentifierOwner
) : ClassSourceMark(sourceFileMarker, psiClass), GutterMark {

    override val id: String = UUID.randomUUID().toString()

    final override val configuration: GutterMarkConfiguration = SourceMarker.configuration.gutterMarkConfiguration
    private var visible: AtomicBoolean = AtomicBoolean(SourceMarkerVisibilityAction.globalVisibility)

    override fun isVisible(): Boolean {
        return visible.get()
    }

    override fun setVisible(visible: Boolean) {
        val previousVisibility = this.visible.getAndSet(visible)
        if (visible && !previousVisibility) {
            triggerEvent(SourceMarkEvent(this, GutterMarkEventCode.GUTTER_MARK_VISIBLE))
        } else if (!visible && previousVisibility) {
            triggerEvent(SourceMarkEvent(this, GutterMarkEventCode.GUTTER_MARK_HIDDEN))
        }
    }
}
