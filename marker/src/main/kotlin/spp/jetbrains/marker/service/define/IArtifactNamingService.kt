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
package spp.jetbrains.marker.service.define

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import spp.jetbrains.artifact.service.define.ISourceMarkerService
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.protocol.artifact.ArtifactLanguage
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.exception.LiveStackTraceElement
import spp.protocol.instrument.location.LiveSourceLocation

/**
 * Responsible for determining the naming/location of source code artifacts.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface IArtifactNamingService : ISourceMarkerService {

    fun getLiveSourceLocation(
        sourceMark: SourceMark,
        lineNumber: Int,
        serviceName: String?
    ): LiveSourceLocation?

    fun getDisplayLocation(language: Language, artifactQualifiedName: ArtifactQualifiedName): String
    fun getVariableName(element: PsiElement): String?
    fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName

    /**
     * Find a [PsiFile] by its [location] in the given [project].
     *
     * @param language the language of the file
     * @param project the project to search in
     * @param location the location of the file (e.g. file path / qualified name)
     */
    fun findPsiFile(language: ArtifactLanguage, project: Project, location: String): PsiFile? {
        val virtualFiles = FilenameIndex.getVirtualFilesByName(
            location.substringAfterLast("/"),
            GlobalSearchScope.projectScope(project)
        )
        return virtualFiles
            .firstOrNull { it.path.endsWith(location) }
            ?.let { PsiManager.getInstance(project).findFile(it) }
    }

    fun findPsiFile(language: ArtifactLanguage, project: Project, frame: LiveStackTraceElement): PsiFile?
}
