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

    var useHeavyPopup = true
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
