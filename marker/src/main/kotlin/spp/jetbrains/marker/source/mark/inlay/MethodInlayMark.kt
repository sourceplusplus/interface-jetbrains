package spp.jetbrains.marker.source.mark.inlay

import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.plugin.SourceMarkerVisibilityAction
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkConfiguration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import spp.jetbrains.marker.SourceMarker.configuration as pluginConfiguration

/**
 * Represents an [InlayMark] associated to a method artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class MethodInlayMark @JvmOverloads constructor(
    override val sourceFileMarker: SourceFileMarker,
    override var psiMethod: PsiNameIdentifierOwner,
    override val configuration: InlayMarkConfiguration = pluginConfiguration.inlayMarkConfiguration.copy(),
) : MethodSourceMark(sourceFileMarker, psiMethod), InlayMark {

    override val id: String = UUID.randomUUID().toString()
    override var visible: AtomicBoolean = AtomicBoolean(SourceMarkerVisibilityAction.globalVisibility)
}
