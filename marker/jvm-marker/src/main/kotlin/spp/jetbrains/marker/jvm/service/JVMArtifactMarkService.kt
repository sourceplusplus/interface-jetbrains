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
package spp.jetbrains.marker.jvm.service

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.*
import com.intellij.ui.treeStructure.SimpleNode
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.jvm.presentation.JVMVariableNode
import spp.jetbrains.marker.jvm.service.utils.JVMMarkerUtils
import spp.jetbrains.marker.service.ArtifactCreationService
import spp.jetbrains.marker.service.ArtifactMarkService
import spp.jetbrains.marker.service.define.IArtifactMarkService
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.ClassGutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.InlayMark
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

    override fun getLineMarkerInfo(
        parent: PsiElement?,
        element: PsiElement
    ): LineMarkerInfo<PsiElement>? {
        return when {
            parent is PsiClass && element === parent.nameIdentifier -> getClassGutterMark(element)
            parent is PsiMethod && element === parent.nameIdentifier -> getMethodGutterMark(element)
            parent is GrMethod && element === parent.nameIdentifier -> getMethodGutterMark(element)
            parent is KtFunction && element === parent.nameIdentifier -> getMethodGutterMark(element)
            else -> null
        }
    }

    private fun getClassGutterMark(element: PsiIdentifier): LineMarkerInfo<PsiElement>? {
        val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile) ?: return null
        val artifactQualifiedName = JVMMarkerUtils.getFullyQualifiedName(element.parent.toUElement() as UClass)

        //check by artifact name first due to user can erroneously name same class twice
        var gutterMark = fileMarker.getSourceMark(artifactQualifiedName, SourceMark.Type.GUTTER) as ClassGutterMark?
        if (gutterMark == null) {
            gutterMark = JVMMarkerUtils.getOrCreateClassGutterMark(fileMarker, element) ?: return null
        }
        if (!gutterMark.isVisible()) {
            return null
        }

        var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
        if (gutterMark.configuration.activateOnMouseClick) {
            navigationHandler = GutterIconNavigationHandler { _, _ ->
                element.getUserData(SourceKey.GutterMark)!!.displayPopup()
            }
        }
        return LineMarkerInfo(
            ArtifactMarkService.getFirstLeaf(element),
            element.textRange,
            gutterMark.configuration.icon,
            null,
            navigationHandler,
            GutterIconRenderer.Alignment.CENTER
        )
    }

    private fun getMethodGutterMark(element: PsiElement): LineMarkerInfo<PsiElement>? {
        val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile) ?: return null
        val artifactQualifiedName = JVMMarkerUtils.getFullyQualifiedName(element)

        //check by artifact name first due to user can erroneously name same method twice
        var gutterMark = fileMarker.getSourceMark(
            artifactQualifiedName,
            SourceMark.Type.GUTTER
        ) as MethodGutterMark?
        if (gutterMark == null) {
            gutterMark = ArtifactCreationService.getOrCreateMethodGutterMark(fileMarker, element) ?: return null
        }
        if (!gutterMark.isVisible()) {
            return null
        }

        var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
        if (gutterMark.configuration.activateOnMouseClick) {
            navigationHandler = GutterIconNavigationHandler { _, _ ->
                element.getUserData(SourceKey.GutterMark)!!.displayPopup()
            }
        }
        return LineMarkerInfo(
            ArtifactMarkService.getFirstLeaf(element),
            element.textRange,
            gutterMark.configuration.icon,
            null,
            navigationHandler,
            GutterIconRenderer.Alignment.CENTER
        )
    }

    override fun createInlayMarkIfNecessary(element: PsiElement): InlayMark? {
        val parent = element.parent
        if ((parent is PsiMethod && element === parent.nameIdentifier)
            || (JVMMarkerUtils.getNameIdentifier(parent) === element)
        ) {
            val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile)!!
            return JVMMarkerUtils.getOrCreateMethodInlayMark(fileMarker, element)
        } else if (element is PsiStatement) {
            val fileMarker = SourceMarker.getSourceFileMarker(element.containingFile)!!
            return JVMMarkerUtils.getOrCreateExpressionInlayMark(fileMarker, element)
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
