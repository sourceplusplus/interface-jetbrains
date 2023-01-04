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
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.CENTER
import com.intellij.psi.PsiElement
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
        val gutterMark = element.getUserData(GutterMark.KEY) ?: return null
        if (!gutterMark.isVisible()) {
            return null
        }

        var navigationHandler: GutterIconNavigationHandler<PsiElement>? = null
        if (gutterMark.configuration.activateOnMouseClick) {
            navigationHandler = GutterIconNavigationHandler { _, _ ->
                element.getUserData(GutterMark.KEY)!!.displayPopup()
            }
        }
        return LineMarkerInfo(
            ArtifactMarkService.getFirstLeaf(element),
            element.textRange,
            gutterMark.configuration.icon,
            gutterMark.configuration.tooltipText.let { tooltipText ->
                if (tooltipText != null) {
                    Function { tooltipText.invoke() }
                } else {
                    null
                }
            },
            navigationHandler,
            CENTER
        )
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

    override fun getName(): String = "Source++ line markers"
}
