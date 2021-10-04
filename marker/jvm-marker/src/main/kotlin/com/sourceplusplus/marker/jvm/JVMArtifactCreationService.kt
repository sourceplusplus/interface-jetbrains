package com.sourceplusplus.marker.jvm

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiStatement
import com.sourceplusplus.marker.ArtifactCreationService
import com.sourceplusplus.marker.SourceMarkerUtils
import com.sourceplusplus.marker.source.JVMMarkerUtils
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.gutter.ExpressionGutterMark
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactCreationService : ArtifactCreationService {

    override fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionGutterMark> {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)
        return if (element is PsiStatement) {
            Optional.ofNullable(JVMMarkerUtils.getOrCreateExpressionGutterMark(fileMarker, element, autoApply))
        } else Optional.empty()
    }

    override fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionInlayMark> {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)
        return if (element is PsiStatement) {
            Optional.ofNullable(JVMMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, element, autoApply))
        } else if (element is PsiElement) {
            Optional.ofNullable(JVMMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, element, autoApply))
        } else {
            Optional.empty()
        }
    }

    override fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionInlayMark {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber) as PsiStatement
        return JVMMarkerUtils.createExpressionInlayMark(fileMarker, element, autoApply)
    }
}
