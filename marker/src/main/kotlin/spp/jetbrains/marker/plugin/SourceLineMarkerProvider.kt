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
package spp.jetbrains.marker.plugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.CENTER
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.key.SourceKey
import spp.jetbrains.marker.source.mark.gutter.GutterMark

/**
 * Used to associate [GutterMark]s with IntelliJ PSI elements.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class SourceLineMarkerProvider : LineMarkerProviderDescriptor() {

    abstract fun getLineMarkerInfo(parent: PsiElement?, element: PsiElement): LineMarkerInfo<PsiElement>?

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (!SourceMarker.getInstance(element.project).enabled) {
            return null
        }

        val parent = element.parent
        val el = getLineMarkerInfo(parent, element)
        if (el == null) {
            //expression gutter marks
            //todo: only works for manually created expression gutter marks atm
            if (element.getUserData(SourceKey.GutterMark) != null) {
                val gutterMark = element.getUserData(SourceKey.GutterMark)!!
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
                    getFirstLeaf(element),
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
        }

        return el
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.firstOrNull() == null || !SourceMarker.getInstance(elements.first().project).enabled) {
            return
        }

        elements.stream().map { it.containingFile }.distinct().forEach {
            SourceMarker.getInstance(it.project).getSourceFileMarker(it)?.removeInvalidSourceMarks()
        }
    }

    fun getFirstLeaf(element: PsiElement): PsiElement {
        var e = element
        while (e.children.isNotEmpty()) {
            e = e.firstChild
        }
        return e
    }
}
