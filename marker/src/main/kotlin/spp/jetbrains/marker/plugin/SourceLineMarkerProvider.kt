/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.marker.plugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.LINE_NUMBERS
import com.intellij.psi.*
import com.intellij.util.Function
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.service.ArtifactMarkService
import spp.jetbrains.marker.source.mark.gutter.GutterMark

/**
 * Used to associate [GutterMark]s with IntelliJ PSI elements.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class SourceLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (element is PsiAnnotation && element.owner is PsiModifierList) {
            val owner = element.owner as PsiModifierList
            if (owner.parent is PsiNameIdentifierOwner) {
                val gutterMark = (owner.parent as PsiNameIdentifierOwner).nameIdentifier
                    ?.getUserData(GutterMark.KEY)
                if (gutterMark?.configuration?.preferShowOnAnnotations == true) {
                    return getLineMarkerInfo(gutterMark, element)
                }
            }
        }

        val gutterMark = element.getUserData(GutterMark.KEY) ?: return null
        val hasAnnotations = (gutterMark.getPsiElement() as? PsiModifierListOwner)?.annotations?.isNotEmpty() == true
        return if (hasAnnotations && gutterMark.configuration.preferShowOnAnnotations) {
            null
        } else {
            getLineMarkerInfo(gutterMark, element)
        }
    }

    private fun getLineMarkerInfo(gutterMark: GutterMark, element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (!gutterMark.isVisible()) {
            return null
        }
        val icon = gutterMark.configuration.icon ?: return null

        var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
        if (gutterMark.configuration.navigationHandler != null) {
            navigationHandler = gutterMark.configuration.navigationHandler
        } else if (gutterMark.configuration.activateOnMouseClick) {
            navigationHandler = GutterIconNavigationHandler { _, _ ->
                element.getUserData(GutterMark.KEY)!!.displayPopup()
            }
        }

        return LineMarkerInfo(
            ArtifactMarkService.getFirstLeaf(element),
            element.textRange,
            icon,
            gutterMark.configuration.tooltipText.let { tooltipText ->
                if (tooltipText != null) {
                    Function { tooltipText.invoke() }
                } else {
                    null
                }
            },
            navigationHandler,
            LINE_NUMBERS //todo: config
        ) { "spp.line-marker" }
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.firstOrNull() == null) {
            return
        }

        elements.stream().map { it.containingFile }.distinct().forEach {
            SourceMarker.getSourceFileMarker(it)?.removeInvalidSourceMarks()
        }
    }

    override fun getName(): String = "Line markers"
}
