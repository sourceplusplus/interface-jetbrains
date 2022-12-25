/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.marker.jvm.service

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiStatement
import com.intellij.ui.treeStructure.SimpleNode
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.jvm.presentation.JVMVariableNode
import spp.jetbrains.marker.jvm.service.utils.JVMMarkerUtils
import spp.jetbrains.marker.service.ArtifactTypeService
import spp.jetbrains.marker.service.define.IArtifactMarkService
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkVirtualText
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.instrument.variable.LiveVariable

/**
 * Used to display JVM [SourceMark]s & [LiveVariable]s.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JVMArtifactMarkService : IArtifactMarkService {

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
            if (ArtifactTypeService.isFunction(statement.parent)) {
                virtualText.spacingTillMethodText = SourceMarkerUtils.getPrefixSpacingCount(statement.parent)
            }

            var startOffset = statement.textRange.startOffset
            if (virtualText.showBeforeAnnotationsWhenBlock) {
                if (ArtifactTypeService.isFunction(statement.parent)) {
                    val annotations = JVMMarkerUtils.getMethodAnnotations(statement.parent)
                    if (annotations.isNotEmpty()) {
                        startOffset = annotations[0].textRange.startOffset
                    }
                }
            }
            sink.addBlockElement(
                startOffset,
                virtualText.relatesToPrecedingText,
                virtualText.showAbove,
                virtualText.priority,
                representation
            )
        }
    }

    override fun toPresentationNodes(language: ArtifactLanguage, vars: List<LiveVariable>): Array<SimpleNode> {
        val simpleNodeMap: MutableMap<String, JVMVariableNode> = LinkedHashMap()
        val nodeReferenceMap = mutableMapOf<String, Array<SimpleNode>>()
        vars.forEach {
            if (it.name.isNotEmpty()) {
                simpleNodeMap[it.name] = JVMVariableNode(it, nodeReferenceMap)
            }
        }
        return simpleNodeMap.values.sortedWith { p0, p1 ->
            when {
                p0.variable.name == "this" -> -1
                p1.variable.name == "this" -> 1
                else -> 0
            }
        }.toTypedArray()
    }
}
