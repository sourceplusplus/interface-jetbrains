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
import spp.jetbrains.marker.AbstractArtifactScopeService
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.4.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactScopeService : AbstractArtifactScopeService {

    private val services = mutableMapOf<String, AbstractArtifactScopeService>()

    fun addService(scopeService: AbstractArtifactScopeService, language: String, vararg languages: String) {
        services[language] = scopeService
        languages.forEach { services[it] = scopeService }
    }

    private fun getService(language: String): AbstractArtifactScopeService {
        return services[language] ?: throw IllegalArgumentException("No service for language $language")
    }

    override fun getScopeVariables(fileMarker: SourceFileMarker, lineNumber: Int): List<String> {
        return getService(fileMarker.psiFile.language.id).getScopeVariables(fileMarker, lineNumber)
    }

    override fun isInsideFunction(element: PsiElement): Boolean {
        return getService(element.language.id).isInsideFunction(element)
    }

    override fun isInsideEndlessLoop(element: PsiElement): Boolean {
        return getService(element.language.id).isInsideEndlessLoop(element)
    }
}
