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
package spp.jetbrains.marker.service

import com.intellij.psi.PsiElement
import spp.jetbrains.artifact.service.define.AbstractSourceMarkerService
import spp.jetbrains.marker.service.define.IArtifactCreationService
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.guide.ExpressionGuideMark
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
object ArtifactCreationService : AbstractSourceMarkerService<IArtifactCreationService>(), IArtifactCreationService {

    override fun getOrCreateExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionGutterMark> {
        return getService(fileMarker.psiFile.language)
            .getOrCreateExpressionGutterMark(fileMarker, lineNumber, autoApply)
    }

    override fun getOrCreateMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark? {
        return getService(fileMarker.psiFile.language)
            .getOrCreateMethodGutterMark(fileMarker, element, autoApply)
    }

    override fun getOrCreateExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): Optional<ExpressionInlayMark> {
        return getService(fileMarker.psiFile.language)
            .getOrCreateExpressionInlayMark(fileMarker, lineNumber, autoApply)
    }

    fun createMethodGutterMark(
        sourceMark: MethodSourceMark,
        autoApply: Boolean
    ): MethodGutterMark {
        return createMethodGutterMark(sourceMark.sourceFileMarker, sourceMark.getNameIdentifier(), autoApply)
    }

    override fun createMethodGutterMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodGutterMark {
        return getService(fileMarker.psiFile.language)
            .createMethodGutterMark(fileMarker, element, autoApply)
    }

    override fun createMethodInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): MethodInlayMark {
        return getService(fileMarker.psiFile.language)
            .createMethodInlayMark(fileMarker, element, autoApply)
    }

    fun createExpressionGutterMark(
        sourceMark: ExpressionSourceMark,
        autoApply: Boolean
    ): ExpressionGutterMark {
        return createExpressionGutterMark(sourceMark.sourceFileMarker, sourceMark.lineNumber, autoApply)
    }

    override fun createExpressionGutterMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionGutterMark {
        return getService(fileMarker.psiFile.language)
            .createExpressionGutterMark(fileMarker, lineNumber, autoApply)
    }

    override fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionInlayMark {
        return getService(fileMarker.psiFile.language)
            .createExpressionInlayMark(fileMarker, lineNumber, autoApply)
    }

    override fun createExpressionInlayMark(
        fileMarker: SourceFileMarker,
        element: PsiElement,
        autoApply: Boolean
    ): ExpressionInlayMark {
        return getService(fileMarker.psiFile.language)
            .createExpressionInlayMark(fileMarker, element, autoApply)
    }

    override fun createExpressionGuideMark(
        fileMarker: SourceFileMarker,
        lineNumber: Int,
        autoApply: Boolean
    ): ExpressionGuideMark {
        return getService(fileMarker.psiFile.language)
            .createExpressionGuideMark(fileMarker, lineNumber, autoApply)
    }
}
