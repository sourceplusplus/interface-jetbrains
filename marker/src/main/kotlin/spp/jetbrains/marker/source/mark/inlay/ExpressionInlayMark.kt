/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2024 CodeBrig, Inc.
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
package spp.jetbrains.marker.source.mark.inlay

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.plugin.action.SourceMarkerVisibilityAction
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.api.ExpressionSourceMark
import spp.protocol.artifact.ArtifactType
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents an [InlayMark] associated to an expression artifact.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ExpressionInlayMark constructor(
    override val sourceFileMarker: SourceFileMarker,
    psiExpression: PsiElement
) : ExpressionSourceMark(sourceFileMarker, psiExpression), InlayMark {

    override val id = UUID.randomUUID().toString()
    override val configuration = SourceMarker.getInstance(project).configuration.inlayMarkConfiguration.copy()
    override var visible = AtomicBoolean(SourceMarkerVisibilityAction.globalVisibility)
    var showAboveExpression = false

    init {
        //show above element by default for method and class artifacts
        if (artifactQualifiedName.type in listOf(ArtifactType.FUNCTION, ArtifactType.CLASS)) {
            showAboveExpression = true
        }
    }
}
