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
