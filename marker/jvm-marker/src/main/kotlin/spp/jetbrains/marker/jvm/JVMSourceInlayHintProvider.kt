/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains.marker.jvm

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiStatement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.SourceInlayHintProvider
import spp.jetbrains.marker.source.JVMMarkerUtils
import spp.jetbrains.marker.source.mark.inlay.InlayMark
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkVirtualText

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
            || (JVMMarkerUtils.getNameIdentifier(parent) === element)
        ) {
            val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile)!!
            val artifactQualifiedName = JVMMarkerUtils.getFullyQualifiedName(parent.toUElement() as UMethod)
            return if (!SourceMarker.configuration.createSourceMarkFilter.test(artifactQualifiedName)) null else {
                JVMMarkerUtils.getOrCreateMethodInlayMark(fileMarker, element)
            }
        } else if (element is PsiStatement) {
            val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile)!!
            val artifactQualifiedName = JVMMarkerUtils.getFullyQualifiedName(
                JVMMarkerUtils.getUniversalExpression(element).toUElement() as UExpression
            )
            return if (!SourceMarker.configuration.createSourceMarkFilter.test(artifactQualifiedName)) null else {
                JVMMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, element)
            }
        }
        return null
    }

    override fun displayVirtualText(
        element: PsiElement,
        virtualText: InlayMarkVirtualText,
        sink: InlayHintsSink,
        representation: InlayPresentation
    ) {
        val statement = if (element is PsiStatement) element else element
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
                0,
                representation
            )
        }
    }
}
