package spp.jetbrains.marker.py

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.psi.PsiElement
import spp.jetbrains.marker.plugin.SourceInlayHintProvider
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkVirtualText

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonSourceInlayHintProvider : SourceInlayHintProvider() {

    override fun createInlayMarkIfNecessary(element: PsiElement): InlayMark? {
        TODO("Not yet implemented")
    }

    override fun displayVirtualText(
        element: PsiElement,
        virtualText: InlayMarkVirtualText,
        sink: InlayHintsSink,
        representation: InlayPresentation
    ) {
        TODO("Not yet implemented")
    }
}
