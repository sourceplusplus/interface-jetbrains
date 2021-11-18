package spp.jetbrains.marker.source.mark.gutter

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.plugin.SourceMarkerVisibilityAction
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.jetbrains.marker.source.mark.api.event.SourceMarkEvent
import spp.jetbrains.marker.source.mark.gutter.config.GutterMarkConfiguration
import spp.jetbrains.marker.source.mark.gutter.event.GutterMarkEventCode
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import spp.jetbrains.marker.SourceMarker.configuration as pluginConfiguration

/**
 * Represents a [GutterMark] associated to an expression artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class ExpressionGutterMark(
    override val sourceFileMarker: SourceFileMarker,
    override var psiExpression: PsiElement
) : ExpressionSourceMark(sourceFileMarker, psiExpression), GutterMark {

    override val id: String = UUID.randomUUID().toString()
    override val configuration: GutterMarkConfiguration = pluginConfiguration.gutterMarkConfiguration.copy()
    private var visible: AtomicBoolean = AtomicBoolean(SourceMarkerVisibilityAction.globalVisibility)

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
