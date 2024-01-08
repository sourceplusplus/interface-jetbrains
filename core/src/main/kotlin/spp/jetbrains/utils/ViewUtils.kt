/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.utils

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
