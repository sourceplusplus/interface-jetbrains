package spp.jetbrains.marker.source.mark.inlay

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.plugin.SourceMarkerVisibilityAction
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkConfiguration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import spp.jetbrains.marker.SourceMarker.configuration as pluginConfiguration

/**
 * Represents an [InlayMark] associated to an expression artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class ExpressionInlayMark @JvmOverloads constructor(
    override val sourceFileMarker: SourceFileMarker,
    override var psiExpression: PsiElement,
    override val configuration: InlayMarkConfiguration = pluginConfiguration.inlayMarkConfiguration.copy()
) : ExpressionSourceMark(sourceFileMarker, psiExpression), InlayMark {

    override val id: String = UUID.randomUUID().toString()
    override var visible: AtomicBoolean = AtomicBoolean(SourceMarkerVisibilityAction.globalVisibility)
    var showAboveExpression = false
}
