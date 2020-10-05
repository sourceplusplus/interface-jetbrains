package com.sourceplusplus.marker.source.mark.gutter

import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.ExpressionSourceMark
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.gutter.config.GutterMarkConfiguration
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode
import org.jetbrains.uast.UExpression
import java.util.concurrent.atomic.AtomicBoolean
import com.sourceplusplus.marker.plugin.SourceMarkerPlugin.configuration as pluginConfiguration

/**
 * Represents a [GutterMark] associated to an expression artifact.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class ExpressionGutterMark(
    override val sourceFileMarker: SourceFileMarker,
    override var psiExpression: UExpression
) : ExpressionSourceMark(sourceFileMarker, psiExpression), GutterMark {

    override val configuration: GutterMarkConfiguration = pluginConfiguration.defaultGutterMarkConfiguration.copy()
    private var visible: AtomicBoolean = AtomicBoolean()

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
