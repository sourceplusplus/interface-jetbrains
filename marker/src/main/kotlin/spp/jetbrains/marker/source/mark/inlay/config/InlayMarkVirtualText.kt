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
package spp.jetbrains.marker.source.mark.inlay.config

import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.TextAttributes
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode
import java.awt.Point
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
@Suppress("MagicNumber")
open class InlayMarkVirtualText {

    internal var inlayMark: InlayMark? = null
    private var virtualText: String
    val textAttributes: TextAttributes = TextAttributes()
    var icon: Icon? = null
    val iconLocation: Point = Point(0, 3)
    var relatesToPrecedingText: Boolean = false
    var showAbove: Boolean = true
    var useInlinePresentation: Boolean = false
    var showAfterLastChildWhenInline: Boolean = true
    var showBeforeAnnotationsWhenBlock: Boolean = true
    var spacingTillMethodText = 0
    var autoAddingSpacingTillMethodText = true

    constructor(inlayMark: InlayMark, virtualText: String) {
        this.inlayMark = inlayMark
        this.virtualText = virtualText

        ApplicationManager.getApplication().invokeLater {
            InlayHintsPassFactory.forceHintsUpdateOnNextPass()
        }
    }

    constructor(virtualText: String) {
        this.virtualText = virtualText
    }

    fun getVirtualText(): String {
        return virtualText
    }

    fun getRenderedVirtualText(): String {
        return if (autoAddingSpacingTillMethodText) {
            " ".repeat(spacingTillMethodText) + virtualText
        } else {
            virtualText
        }
    }

    fun updateVirtualText(virtualText: String) {
        if (inlayMark != null) {
            val previousVirtualText = this.virtualText
            this.virtualText = virtualText

            inlayMark!!.triggerEvent(
                SourceMarkEvent(
                    inlayMark!!,
                    InlayMarkEventCode.VIRTUAL_TEXT_UPDATED,
                    previousVirtualText,
                    virtualText
                )
            )
        }
    }
}
