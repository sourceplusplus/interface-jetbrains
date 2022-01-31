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
package spp.jetbrains.marker.source.mark.api.component.api.config

import com.intellij.openapi.editor.Editor
import spp.jetbrains.marker.source.mark.api.component.api.SourceMarkComponent
import java.awt.Dimension

/**
 * Used to configure [SourceMarkComponent].
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class SourceMarkComponentConfiguration {

    var useHeavyPopup = false
    var hideOnMouseMotion = false
    var hideOnScroll = true
    var showAboveClass: Boolean = false
    var showAboveMethod: Boolean = true
    var showAboveExpression: Boolean = false
    var componentSizeEvaluator: ComponentSizeEvaluator = ComponentSizeEvaluator()
    internal var addedMouseMotionListener: Boolean = false
    internal var addedScrollListener: Boolean = false

    open fun copy(): SourceMarkComponentConfiguration {
        val copy = SourceMarkComponentConfiguration()
        copy.useHeavyPopup = useHeavyPopup
        copy.hideOnMouseMotion = hideOnMouseMotion
        copy.hideOnScroll = hideOnScroll
        copy.showAboveClass = showAboveClass
        copy.showAboveMethod = showAboveMethod
        copy.showAboveExpression = showAboveExpression
        copy.componentSizeEvaluator = componentSizeEvaluator
        return copy
    }
}

/**
 * Allows a [SourceMarkComponent] to be dynamically sized.
 */
open class ComponentSizeEvaluator {
    open fun getDynamicSize(editor: Editor, configuration: SourceMarkComponentConfiguration): Dimension? {
        return null
    }
}
