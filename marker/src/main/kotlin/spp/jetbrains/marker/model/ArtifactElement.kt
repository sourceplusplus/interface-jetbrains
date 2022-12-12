/*
 * Source++, the continuous feedback platform for developers.
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
package spp.jetbrains.marker.model

import com.intellij.psi.PsiElement
import spp.jetbrains.marker.service.ArtifactTypeService
import spp.jetbrains.marker.service.getCalls
import spp.jetbrains.marker.service.toArtifact

open class ArtifactElement(private val psiElement: PsiElement) : PsiElement by psiElement {
    fun isControlStructure(): Boolean = this is ControlStructureArtifact
    fun isCall(): Boolean = this is CallArtifact
}

// Extensions

fun ArtifactElement?.isLiteral(): Boolean {
    return (this != null) && ArtifactTypeService.isLiteral(this)
}

fun ArtifactElement.getCalls(): List<CallArtifact> {
    return getCalls().map { it.toArtifact() }.filterIsInstance<CallArtifact>()
}
