/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.marker.source.mark.inlay.config

import com.intellij.codeInsight.codeVision.ui.model.richText.RichText
import com.intellij.openapi.editor.markup.TextAttributes
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.marker.source.mark.inlay.event.InlayMarkEventCode
import java.awt.Font
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
    var richText: RichText? = null
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
    var font: Font? = null
    var fontSize: Float? = null
    var relativeFontSize: Boolean = false
    var xOffset: Int = 0
    var priority: Int = 0

    constructor(inlayMark: InlayMark, virtualText: String) {
        this.inlayMark = inlayMark
        this.virtualText = virtualText
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

    fun updateVirtualText(richText: RichText) {
        require(inlayMark != null) { "InlayMark must be set before updating virtual text" }

        val previousRichText = this.richText
        this.richText = richText

        inlayMark!!.triggerEvent(
            SourceMarkEvent(
                inlayMark!!,
                InlayMarkEventCode.VIRTUAL_TEXT_UPDATED,
                previousRichText,
                richText
            )
        )
    }
}
