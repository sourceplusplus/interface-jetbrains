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
object ArtifactNamingService : AbstractArtifactNamingService {

    private val services = mutableMapOf<String, AbstractArtifactNamingService>()

    fun addService(namingService: AbstractArtifactNamingService, language: String, vararg languages: String) {
        services[language] = namingService
        languages.forEach { services[it] = namingService }
    }

    private fun getService(language: String): AbstractArtifactNamingService {
        return services[language] ?: throw IllegalArgumentException("No service for language $language")
    }

    override fun getVariableName(element: PsiElement): String? {
        return getService(element.language.id).getVariableName(element)
    }

    override fun getFullyQualifiedName(element: PsiElement): ArtifactQualifiedName {
        return getService(element.language.id).getFullyQualifiedName(element)
    }

    override fun getQualifiedClassNames(psiFile: PsiFile): List<ArtifactQualifiedName> {
        return getService(psiFile.language.id).getQualifiedClassNames(psiFile)
    }
}
