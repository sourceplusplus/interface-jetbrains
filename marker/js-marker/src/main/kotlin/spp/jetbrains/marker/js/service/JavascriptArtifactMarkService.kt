/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.marker.js.service

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSStatement
import com.intellij.psi.PsiElement
import com.intellij.ui.treeStructure.SimpleNode
import spp.jetbrains.marker.SourceMarkerUtils
import spp.jetbrains.marker.js.presentation.JavascriptVariableRootNode
import spp.jetbrains.marker.service.define.IArtifactMarkService
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkVirtualText
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.instrument.variable.LiveVariable
import spp.protocol.instrument.variable.LiveVariableScope

/**
 * Used to display JavaScript [SourceMark]s & [LiveVariable]s.
 *
 * @since 0.7.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class JavascriptArtifactMarkService : IArtifactMarkService {

    override fun displayVirtualText(
        element: PsiElement,
        virtualText: InlayMarkVirtualText,
        sink: InlayHintsSink,
        representation: InlayPresentation
    ) {
        var statement = if (element is JSStatement) element else element
        if (virtualText.useInlinePresentation) {
            if (virtualText.showAfterLastChildWhenInline) {
                if (statement is JSCallExpression) {
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
            if (statement.parent is JSFunction) {
                virtualText.spacingTillMethodText = SourceMarkerUtils.getPrefixSpacingCount(statement.parent)
            }

            var startOffset = statement.textRange.startOffset
//            if (virtualText.showBeforeAnnotationsWhenBlock) {
//                if (statement.parent is JSFunction) {
//                    val annotations = (statement.parent as JSFunction).decoratorList?.decorators ?: emptyArray()
//                    if (annotations.isNotEmpty()) {
//                        startOffset = annotations[0].textRange.startOffset
//                    }
//                }
//            }
            sink.addBlockElement(
                startOffset,
                virtualText.relatesToPrecedingText,
                virtualText.showAbove,
                0,
                representation
            )
        }
    }

    override fun toPresentationNodes(language: ArtifactLanguage, vars: List<LiveVariable>): Array<SimpleNode> {
        return arrayOf(
            JavascriptVariableRootNode(
                vars.filter { it.scope == LiveVariableScope.LOCAL_VARIABLE },
                LiveVariableScope.LOCAL_VARIABLE
            ),
            JavascriptVariableRootNode(
                vars.filter { it.scope == LiveVariableScope.GLOBAL_VARIABLE },
                LiveVariableScope.GLOBAL_VARIABLE
            )
        )
    }
}
