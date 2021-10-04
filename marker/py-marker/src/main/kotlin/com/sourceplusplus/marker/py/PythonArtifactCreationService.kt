package com.sourceplusplus.marker.py

import com.intellij.psi.PsiElement
import com.sourceplusplus.marker.ArtifactCreationService
import com.sourceplusplus.marker.Utils
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactCreationService : ArtifactCreationService {

    override fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionInlayMark> {
        val element = Utils.getElementAtLine(fileMarker.psiFile, lineNumber)
        if (element != null) {//PyExpressionStatement
            return Optional.ofNullable(getOrCreateExpressionInlayMark(fileMarker, element, autoApply))
        }
        return Optional.empty()
    }

    override fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionInlayMark {
        val element = Utils.getElementAtLine(fileMarker.psiFile, lineNumber)!!
        val inlayMark = fileMarker.createExpressionSourceMark(
            element,
            SourceMark.Type.INLAY
        ) as ExpressionInlayMark
        return if (autoApply) {
            if (inlayMark.canApply()) {
                inlayMark.apply(true)
                inlayMark
            } else {
                throw IllegalStateException("Could not apply inlay mark: $inlayMark")
            }
        } else {
            inlayMark
        }
    }

    fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): ExpressionInlayMark? {
        var inlayMark = element.getUserData(SourceKey.InlayMark) as ExpressionInlayMark?
        if (inlayMark == null) {
            inlayMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark?
            if (inlayMark != null) {
                TODO()
//                    if (inlayMark.updatePsiExpression(
//                            statementExpression,
//                            getFullyQualifiedName(statementExpression.toUElement() as UExpression)
//                        )
//                    ) {
//                        statementExpression.putUserData(SourceKey.InlayMark, inlayMark)
//                    } else {
//                        inlayMark = null
//                    }
            }
        }

        return if (inlayMark == null) {
            inlayMark = fileMarker.createExpressionSourceMark(
                element,
                SourceMark.Type.INLAY
            ) as ExpressionInlayMark
            return if (autoApply) {
                if (inlayMark.canApply()) {
                    inlayMark.apply(true)
                    inlayMark
                } else {
                    null
                }
            } else {
                inlayMark
            }
        } else {
            if (fileMarker.removeIfInvalid(inlayMark)) {
                element.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                inlayMark
            }

        }
    }
}
