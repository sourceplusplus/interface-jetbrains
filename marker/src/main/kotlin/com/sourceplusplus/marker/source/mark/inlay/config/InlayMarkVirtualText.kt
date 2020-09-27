package com.sourceplusplus.marker.source.mark.inlay.config

import com.intellij.codeInsight.hints.InlayHintsPassFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.marker.source.mark.inlay.event.InlayMarkEventCode
import java.awt.Point
import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
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
