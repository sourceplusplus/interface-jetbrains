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
package spp.jetbrains.artifact.service.define

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralValue
import spp.jetbrains.artifact.model.ArtifactLiteralValue
import spp.protocol.artifact.ArtifactType

interface IArtifactTypeService : ISourceMarkerService {

    /**
     * Necessary because Groovy uses [LightIdentifier] for the name identifier.
     */
    fun getNameIdentifier(element: PsiElement): PsiElement = element
    fun getAnnotations(element: PsiElement): List<PsiElement>
    fun getAnnotationOwnerIfAnnotation(element: PsiElement): PsiElement?
    fun getAnnotationOwnerIfAnnotation(element: PsiElement, line: Int): PsiElement?
    fun isComment(element: PsiElement): Boolean
    fun getType(element: PsiElement): ArtifactType?

    fun isLiteral(element: PsiElement): Boolean {
        return element is PsiLiteralValue || element is ArtifactLiteralValue
    }
}
