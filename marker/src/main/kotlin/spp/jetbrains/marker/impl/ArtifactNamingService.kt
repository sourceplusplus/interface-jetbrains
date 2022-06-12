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
import com.intellij.psi.PsiFile
import spp.jetbrains.marker.AbstractArtifactNamingService
import spp.protocol.artifact.ArtifactQualifiedName

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactNamingService {

    private val services = mutableMapOf<String, AbstractArtifactNamingService>()

    fun addService(namingService: AbstractArtifactNamingService, language: String, vararg languages: String) {
        services[language] = namingService
        languages.forEach { services[it] = namingService }
    }

    private fun getService(language: String): AbstractArtifactNamingService {
        return services[language] ?: throw IllegalArgumentException("No service for language $language")
    }

    fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return getService(element.language.id).getFullyQualifiedName(element)
    }

    fun getQualifiedClassNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        return getService(psiFile.language.id).getQualifiedClassNames(psiFile)
    }
}
