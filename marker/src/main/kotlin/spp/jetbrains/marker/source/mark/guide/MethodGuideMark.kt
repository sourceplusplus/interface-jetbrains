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
package spp.jetbrains.marker.source.mark.guide

import com.intellij.psi.PsiNameIdentifierOwner
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.MethodSourceMark
import spp.jetbrains.marker.source.mark.guide.config.GuideMarkConfiguration
import java.util.*
import spp.jetbrains.marker.SourceMarker.configuration as pluginConfiguration

/**
 * Represents a [GuideMark] associated to a method artifact.
 *
 * @since 0.4.7
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class MethodGuideMark(
    override val sourceFileMarker: SourceFileMarker,
    override var psiMethod: PsiNameIdentifierOwner
) : MethodSourceMark(sourceFileMarker, psiMethod), GuideMark {

    override val id: String = UUID.randomUUID().toString()
    override val configuration: GuideMarkConfiguration = pluginConfiguration.guideMarkConfiguration.copy()
}
