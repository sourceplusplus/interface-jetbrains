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
package spp.jetbrains.marker.plugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.CENTER
import com.intellij.psi.PsiElement
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(SourceLineMarkerProvider::class.java)

    abstract fun getLineMarkerInfo(parent: PsiElement?, element: PsiElement): LineMarkerInfo<PsiElement>?

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (!SourceMarker.enabled) {
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
                    null,
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
        if (!SourceMarker.enabled) {
            return
        }

        elements.stream().map { it.containingFile }.distinct().forEach {
            SourceMarker.getSourceFileMarker(it)?.removeInvalidSourceMarks()
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
