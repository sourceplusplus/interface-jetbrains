package com.sourceplusplus.marker.jvm

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiStatement
import com.sourceplusplus.marker.ArtifactCreationService
import com.sourceplusplus.marker.Utils
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import java.util.*

class JVMArtifactCreationService : ArtifactCreationService {

    override fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionInlayMark> {
        val element = Utils.getElementAtLine(fileMarker.psiFile, lineNumber)
        return if (element is PsiStatement) {
            Optional.ofNullable(SourceMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, element, autoApply))
        } else if (element is PsiElement) {
            Optional.ofNullable(SourceMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, element, autoApply))
        } else {
            Optional.empty()
        }
    }

    override fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionInlayMark {
        val element = Utils.getElementAtLine(fileMarker.psiFile, lineNumber) as PsiStatement
        return SourceMarkerUtils.createExpressionInlayMark(fileMarker, element, autoApply)
    }
}
