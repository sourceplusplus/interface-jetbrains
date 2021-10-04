package com.sourceplusplus.marker.jvm

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.editor.BlockInlayPriority
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiStatement
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.marker.plugin.SourceInlayHintProvider
import com.sourceplusplus.marker.source.SourceMarkerUtils
import com.sourceplusplus.marker.source.mark.inlay.InlayMark
import com.sourceplusplus.marker.source.mark.inlay.config.InlayMarkVirtualText
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMSourceInlayHintProvider : SourceInlayHintProvider() {

    override fun createInlayMarkIfNecessary(element: PsiElement): InlayMark? {
        val parent = element.parent
        if ((parent is PsiMethod && element === parent.nameIdentifier)
            || (parent is GrMethod && element === parent.nameIdentifierGroovy)
            || (parent is KtNamedFunction && element === parent.nameIdentifier)
        ) {
            val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile)!!
            val artifactQualifiedName = SourceMarkerUtils.getFullyQualifiedName(parent.toUElement() as UMethod)
            return if (!SourceMarker.configuration.createSourceMarkFilter.test(artifactQualifiedName)) null else {
                SourceMarkerUtils.getOrCreateMethodInlayMark(fileMarker, element)
            }
        } else if (element is PsiStatement) {
            val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile)!!
            val artifactQualifiedName = SourceMarkerUtils.getFullyQualifiedName(
                SourceMarkerUtils.getUniversalExpression(element).toUElement() as UExpression
            )
            return if (!SourceMarker.configuration.createSourceMarkFilter.test(artifactQualifiedName)) null else {
                SourceMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, element)
            }
        }
        return null
    }

    override fun doThing(
        element: PsiElement,
        virtualText: InlayMarkVirtualText,
        sink: InlayHintsSink,
        representation: InlayPresentation
    ) {
        val statement = if (element is PsiStatement) element else {
            element.getParentOfType(true)
        }!!
        if (virtualText.useInlinePresentation) {
            if (virtualText.showAfterLastChildWhenInline) {
                sink.addInlineElement(
                    statement.lastChild.textRange.endOffset,
                    virtualText.relatesToPrecedingText,
                    representation
                )
            } else {
                sink.addInlineElement(
                    statement.textRange.startOffset,
                    virtualText.relatesToPrecedingText,
                    representation
                )
            }
        } else {
            if (statement.parent is PsiMethod) {
                virtualText.spacingTillMethodText = statement.parent.prevSibling.text
                    .replace("\n", "").count { it == ' ' }
            }

            var startOffset = statement.textRange.startOffset
            if (virtualText.showBeforeAnnotationsWhenBlock) {
                if (statement.parent is PsiMethod) {
                    val annotations = (statement.parent as PsiMethod).annotations
                    if (annotations.isNotEmpty()) {
                        startOffset = annotations[0].textRange.startOffset
                    }
                }
            }
            sink.addBlockElement(
                startOffset,
                virtualText.relatesToPrecedingText,
                virtualText.showAbove,
                BlockInlayPriority.CODE_VISION,
                representation
            )
        }
    }
}
