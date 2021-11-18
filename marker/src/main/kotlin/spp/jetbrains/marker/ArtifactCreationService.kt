package spp.jetbrains.marker

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.gutter.ExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface ArtifactCreationService {

    fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean = false
    ): Optional<ExpressionGutterMark>

    fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = true //todo: rest are false
    ): MethodGutterMark?

    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean = false
    ): Optional<ExpressionInlayMark>

    fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean = false
    ): ExpressionInlayMark
}
