package com.sourceplusplus.marker.py

import com.intellij.psi.PsiElement
import com.sourceplusplus.marker.ArtifactCreationService
import com.sourceplusplus.marker.SourceMarker.namingService
import com.sourceplusplus.marker.SourceMarkerUtils
import com.sourceplusplus.marker.source.SourceFileMarker
import com.sourceplusplus.marker.source.mark.api.SourceMark
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.marker.source.mark.gutter.ExpressionGutterMark
import com.sourceplusplus.marker.source.mark.gutter.MethodGutterMark
import com.sourceplusplus.marker.source.mark.inlay.ExpressionInlayMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class PythonArtifactCreationService : ArtifactCreationService {

    override fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionGutterMark> {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)
        if (element != null) {//PyExpressionStatement
            return Optional.ofNullable(getOrCreateExpressionGutterMark(fileMarker, element, autoApply))
        }
        return Optional.empty()
    }

    override fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark? {
        TODO()
    }

    fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean = false
    ): ExpressionGutterMark? {
        var gutterMark = element.getUserData(SourceKey.GutterMark) as ExpressionGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getExpressionSourceMark(
                element,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiExpression(element, namingService.getFullyQualifiedName(element))) {
                    element.putUserData(SourceKey.GutterMark, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        return if (gutterMark == null) {
            gutterMark = fileMarker.createExpressionSourceMark(
                element,
                SourceMark.Type.GUTTER
            ) as ExpressionGutterMark
            return if (autoApply) {
                if (gutterMark.canApply()) {
                    gutterMark.apply(true)
                    gutterMark
                } else {
                    null
                }
            } else {
                gutterMark
            }
        } else {
            if (fileMarker.removeIfInvalid(gutterMark)) {
                element.putUserData(SourceKey.InlayMark, null)
                null
            } else {
                gutterMark
            }
        }
    }

    override fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionInlayMark> {
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)
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
        val element = SourceMarkerUtils.getElementAtLine(fileMarker.psiFile, lineNumber)!!
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
