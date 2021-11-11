package com.sourceplusplus.sourcemarker.service.breakpoint

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object BreakpointTriggerListener : EditorMouseListener {

    var shiftHeld = false

    override fun mousePressed(event: EditorMouseEvent) {
        shiftHeld = event.mouseEvent.isShiftDown
    }
}
