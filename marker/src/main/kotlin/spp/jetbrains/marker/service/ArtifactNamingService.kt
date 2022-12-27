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
package spp.jetbrains.marker.service

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import spp.jetbrains.artifact.service.define.AbstractSourceMarkerService
import spp.jetbrains.marker.service.define.IArtifactNamingService
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.instrument.LiveSourceLocation

/**
 * Responsible for determining the naming/location of source code artifacts.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactNamingService : AbstractSourceMarkerService<IArtifactNamingService>(), IArtifactNamingService {

    override fun getLiveSourceLocation(
        sourceMark: SourceMark,
        lineNumber: Int,
        serviceName: String?
    ): LiveSourceLocation? {
        return getService(sourceMark.language).getLiveSourceLocation(sourceMark, lineNumber, serviceName)
    }

    override fun getDisplayLocation(language: Language, artifactQualifiedName: ArtifactQualifiedName): String {
        return getService(language).getDisplayLocation(language, artifactQualifiedName)
    }

    override fun getVariableName(element: PsiElement): String? {
        return getService(element.language).getVariableName(element)
    }

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return getService(element.language).getFullyQualifiedName(element)
    }

    override fun findPsiFile(language: ArtifactLanguage, project: Project, location: String): PsiFile? {
        return getService(language).findPsiFile(language, project, location)
    }

    fun findPsiFile(project: Project, location: String): PsiFile? {
        val availableLanguages = ArtifactLanguage.values().mapNotNull { lang ->
            getServiceIfPresent(lang)?.let { Pair(lang, it) }
        }
        return availableLanguages.firstNotNullOfOrNull { it.second.findPsiFile(it.first, project, location) }
    }

    override fun findPsiFile(language: ArtifactLanguage, project: Project, frame: LiveStackTraceElement): PsiFile? {
        return getService(language).findPsiFile(language, project, frame)
    }
}
