package spp.jetbrains.marker.jvm

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiStatement
import spp.jetbrains.marker.ArtifactCreationService
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.source.JVMMarkerUtils
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.ExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
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

    override fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark? {
        var gutterMark = element.getUserData(SourceKey.GutterMark) as MethodGutterMark?
        if (gutterMark == null) {
            gutterMark = fileMarker.getMethodSourceMark(element.parent, SourceMark.Type.GUTTER) as MethodGutterMark?
            if (gutterMark != null) {
                if (gutterMark.updatePsiMethod(element.parent as PsiNameIdentifierOwner)) {
                    element.putUserData(SourceKey.GutterMark, gutterMark)
                } else {
                    gutterMark = null
                }
            }
        }

        if (gutterMark == null) {
            gutterMark = fileMarker.createMethodSourceMark(
                element.parent as PsiNameIdentifierOwner,
                JVMMarkerUtils.getFullyQualifiedName(element.parent.toUElement() as UMethod),
                SourceMark.Type.GUTTER
            ) as MethodGutterMark
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
            return when {
                fileMarker.removeIfInvalid(gutterMark) -> {
                    element.putUserData(SourceKey.GutterMark, null)
                    null
                }
                gutterMark.configuration.icon != null -> {
                    gutterMark.setVisible(true)
                    gutterMark
                }
                else -> {
                    gutterMark.setVisible(false)
                    gutterMark
                }
            }
        }
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
