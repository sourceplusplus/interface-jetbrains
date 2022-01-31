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
package spp.jetbrains.marker.source.mark.inlay

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.plugin.SourceMarkerVisibilityAction
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.jetbrains.marker.source.mark.inlay.config.InlayMarkConfiguration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import spp.jetbrains.marker.SourceMarker.configuration as pluginConfiguration

/**
 * Represents an [InlayMark] associated to an expression artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class ExpressionInlayMark @JvmOverloads constructor(
    override val sourceFileMarker: SourceFileMarker,
    override var psiExpression: PsiElement,
    override val configuration: InlayMarkConfiguration = pluginConfiguration.inlayMarkConfiguration.copy()
) : ExpressionSourceMark(sourceFileMarker, psiExpression), InlayMark {

    override val id: String = UUID.randomUUID().toString()
    override var visible: AtomicBoolean = AtomicBoolean(SourceMarkerVisibilityAction.globalVisibility)
    var showAboveExpression = false
}
