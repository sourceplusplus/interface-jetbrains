/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.marker.jvm

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiStatement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.SourceInlayHintProvider
import spp.jetbrains.marker.jvm.service.utils.JVMMarkerUtils
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
            val fileMarker = SourceMarker.getInstance(element.project).getSourceFileMarker(element.containingFile)!!
            val artifactQualifiedName = JVMMarkerUtils.getFullyQualifiedName(parent.toUElement() as UMethod)
            return if (!SourceMarker.getInstance(element.project).configuration.createSourceMarkFilter.test(artifactQualifiedName)) null else {
                JVMMarkerUtils.getOrCreateMethodInlayMark(fileMarker, element)
            }
        } else if (element is PsiStatement) {
            val fileMarker = SourceMarker.getInstance(element.project).getSourceFileMarker(element.containingFile)!!
            val artifactQualifiedName = JVMMarkerUtils.getFullyQualifiedName(
                JVMMarkerUtils.getUniversalExpression(element).toUElement() as UExpression
            )
            return if (!SourceMarker.getInstance(element.project).configuration.createSourceMarkFilter.test(artifactQualifiedName)) null else {
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
        var statement = if (element is PsiStatement) element else element
        if (virtualText.useInlinePresentation) {
            if (virtualText.showAfterLastChildWhenInline) {
                if (statement is PsiMethodCallExpression) {
                    statement = statement.parent //todo: more dynamic
                }
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
