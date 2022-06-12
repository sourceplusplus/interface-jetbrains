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

import spp.jetbrains.marker.AbstractSourceGuideProvider
import spp.jetbrains.marker.source.SourceFileMarker

/**
 * todo: description.
 *
 * @since 0.5.5
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object SourceGuideProvider : AbstractSourceGuideProvider {

    private val providers = mutableMapOf<String, AbstractSourceGuideProvider>()

    fun addProvider(guideProvider: AbstractSourceGuideProvider, language: String, vararg languages: String) {
        providers[language] = guideProvider
        languages.forEach { providers[it] = guideProvider }
    }

    private fun getProvider(language: String): AbstractSourceGuideProvider {
        return providers[language] ?: throw IllegalArgumentException("No provider for language $language")
    }

    override fun determineGuideMarks(fileMarker: SourceFileMarker) {
        getProvider(fileMarker.psiFile.language.id).determineGuideMarks(fileMarker)
    }
}
