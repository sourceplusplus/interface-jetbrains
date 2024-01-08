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
package spp.jetbrains.marker.command

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.SourceFileMarker
import spp.jetbrains.marker.source.mark.guide.GuideMark
import spp.jetbrains.marker.source.mark.guide.MethodGuideMark
import spp.protocol.artifact.ArtifactQualifiedName

data class LiveLocationContext(
    val qualifiedName: ArtifactQualifiedName,
    val fileMarker: SourceFileMarker,
    val element: PsiElement,
) {
    fun getFunctionGuideMark(): GuideMark? {
        var qualifiedName: ArtifactQualifiedName? = qualifiedName
        var guideMark: GuideMark? = null
        do {
            if (qualifiedName == null) continue
            guideMark = SourceMarker.getInstance(fileMarker.project).getGuideMark(qualifiedName)

            if (guideMark is MethodGuideMark) {
                return guideMark
            }
            qualifiedName = qualifiedName.asParent()
        } while (guideMark == null && qualifiedName != null)

        return null
    }
}
