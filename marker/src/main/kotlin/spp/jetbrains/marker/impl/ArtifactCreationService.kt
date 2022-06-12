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
package spp.jetbrains.marker.impl

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.AbstractArtifactCreationService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.gutter.ExpressionGutterMark
import spp.jetbrains.marker.source.mark.gutter.MethodGutterMark
import spp.jetbrains.marker.source.mark.inlay.ExpressionInlayMark
import spp.jetbrains.marker.source.mark.inlay.MethodInlayMark
import java.util.*

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactCreationService : AbstractArtifactCreationService {

    private val services = mutableMapOf<String, AbstractArtifactCreationService>()

    fun addService(creationService: AbstractArtifactCreationService, language: String, vararg languages: String) {
        services[language] = creationService
        languages.forEach { services[it] = creationService }
    }

    private fun getService(language: String): AbstractArtifactCreationService {
        return services[language] ?: throw IllegalArgumentException("No service for language $language")
    }

    override fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionGutterMark> {
        return getService(fileMarker.psiFile.language.id)
            .getOrCreateExpressionGutterMark(fileMarker, lineNumber, autoApply)
    }

    override fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark? {
        return getService(fileMarker.psiFile.language.id)
            .getOrCreateMethodGutterMark(fileMarker, element, autoApply)
    }

    override fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionInlayMark> {
        return getService(fileMarker.psiFile.language.id)
            .getOrCreateExpressionInlayMark(fileMarker, lineNumber, autoApply)
    }

    override fun createMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark {
        return getService(fileMarker.psiFile.language.id)
            .createMethodGutterMark(fileMarker, element, autoApply)
    }

    override fun createMethodInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodInlayMark {
        return getService(fileMarker.psiFile.language.id)
            .createMethodInlayMark(fileMarker, element, autoApply)
    }

    override fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionInlayMark {
        return getService(fileMarker.psiFile.language.id)
            .createExpressionInlayMark(fileMarker, lineNumber, autoApply)
    }
}
