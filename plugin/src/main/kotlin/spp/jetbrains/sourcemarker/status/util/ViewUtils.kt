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
package spp.jetbrains.sourcemarker.status.util

import com.intellij.openapi.editor.impl.EditorComponentImpl
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ViewUtils {

    @JvmStatic
    @JvmOverloads
    fun addRecursiveMouseListener(
        component: Component,
        listener: MouseListener,
        onEditorEntered: (() -> Void)? = null
    ) {
        Toolkit.getDefaultToolkit().addAWTEventListener({ event: AWTEvent ->
            if (event is MouseEvent) {
                if (event.component is EditorComponentImpl) {
                    if (event.getID() == MouseEvent.MOUSE_ENTERED) {
                        onEditorEntered?.invoke()
                    }
                } else if (event.component.isShowing && component.isShowing) {
                    if (containsScreenLocation(component, event.locationOnScreen)) {
                        if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                            listener.mousePressed(event)
                        } else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
                            listener.mouseReleased(event)
                        } else if (event.getID() == MouseEvent.MOUSE_ENTERED) {
                            listener.mouseEntered(event)
                        } else if (event.getID() == MouseEvent.MOUSE_EXITED) {
                            listener.mouseExited(event)
                        } else if (event.getID() == MouseEvent.MOUSE_CLICKED) {
                            listener.mouseClicked(event)
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK)
    }

    @JvmStatic
    fun containsScreenLocation(component: Component, screenLocation: Point): Boolean {
        val compLocation = component.locationOnScreen
        val compSize = component.size
        val relativeX = screenLocation.x - compLocation.x
        val relativeY = screenLocation.y - compLocation.y
        return relativeX >= 0 && relativeX < compSize.width && relativeY >= 0 && relativeY < compSize.height
    }
}
