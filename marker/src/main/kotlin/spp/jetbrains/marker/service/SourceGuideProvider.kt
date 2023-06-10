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

import com.intellij.lang.Language
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import spp.jetbrains.marker.service.define.AbstractSourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceGuideProvider : AbstractSourceGuideProvider {

    private val log = logger<SourceGuideProvider>()
    private val providers = ConcurrentHashMap<String, CopyOnWriteArrayList<AbstractSourceGuideProvider>>()

    fun addProvider(guideProvider: AbstractSourceGuideProvider, language: String, vararg languages: String) {
        addProvider(guideProvider, listOf(language, *languages))
    }

    fun addProvider(guideProvider: AbstractSourceGuideProvider, languages: List<String>) {
        languages.forEach { providers.computeIfAbsent(it) { CopyOnWriteArrayList() }.add(guideProvider) }
    }

    private fun getProvider(language: String): AbstractSourceGuideProvider? {
        return providers[language]?.let {
            object : AbstractSourceGuideProvider {
                override fun determineGuideMarks(fileMarker: SourceFileMarker) {
                    it.forEach { provider -> provider.determineGuideMarks(fileMarker) }
                }
            }
        }
    }

    private fun getProvider(language: Language): AbstractSourceGuideProvider? {
        return getProvider(language.baseLanguage?.id ?: language.id)
    }

    override fun determineGuideMarks(fileMarker: SourceFileMarker) {
        ReadAction.nonBlocking<Unit> {
            val guideProvider = getProvider(fileMarker.psiFile.language)
            if (guideProvider != null) {
                guideProvider.determineGuideMarks(fileMarker)
            } else {
                log.warn("No guide provider found for language: ${fileMarker.psiFile.language}")
            }
        }
    }
}
